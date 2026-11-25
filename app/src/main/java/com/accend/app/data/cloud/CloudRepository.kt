package com.accend.app.data.cloud

import android.net.Uri
import com.accend.app.data.db.AccendDatabase
import com.accend.app.data.db.DeepWorkLogEntity
import com.accend.app.data.db.ExerciseLogEntity
import com.accend.app.data.db.MeditationLogEntity
import com.accend.app.data.db.MoneyTransactionEntity
import com.accend.app.data.db.NutritionLogEntity
import com.accend.app.data.db.ReadingLogEntity
import com.accend.app.data.db.ScreenTimeLogEntity
import com.accend.app.util.AccendDate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/** EXP thresholds used across the app, the leaderboard and the Home HUD. */
object LevelCalculator {
    const val EXP_PER_LEVEL = 500L
    const val INITIAL_EXP = 100L

    fun level(exp: Long): Int = (exp / EXP_PER_LEVEL).toInt() + 1
    fun progress(exp: Long): Float = (exp % EXP_PER_LEVEL).toFloat() / EXP_PER_LEVEL.toFloat()
    fun nextLevelAt(exp: Long): Long = (level(exp).toLong() * EXP_PER_LEVEL)
}

data class LeaderboardEntry(
    val uid: String = "",
    val name: String = "",
    val photoUrl: String? = null,
    val exp: Long = 0,
    val rank: Int = 0
)

/** Profile stored in Firestore — the authoritative identity, independent of Firebase Auth. */
data class RemoteProfile(
    val name: String,
    val photoUrl: String?,
    val joinedDate: String?,
    val exists: Boolean
)

/**
 * Single gateway to Firebase Auth, Firestore and FCM.
 *
 * Firestore layout:
 *   users/{uid}                     -> name, displayName, photoUrl, exp, fcmToken, joinedDate
 *   users/{uid}/logs/{yyyy-MM-dd}   -> exercise / nutrition / mind / money snapshots
 */
class CloudRepository(private val db: AccendDatabase) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    val isSignedIn: Boolean get() = auth.currentUser != null
    val currentUid: String? get() = auth.currentUser?.uid
    val currentUser: FirebaseUser? get() = auth.currentUser

    private val users get() = firestore.collection("users")
    private fun logDoc(uid: String, date: String) =
        users.document(uid).collection("logs").document(date)

    fun authState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        trySend(auth.currentUser)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Returns the stored Firestore profile for returning users, or null for brand-new users. */
    suspend fun signInWithGoogle(idToken: String): RemoteProfile? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        val user = auth.currentUser ?: return null
        val snapshot = runCatching { users.document(user.uid).get().await() }.getOrNull()
        val existed = snapshot?.exists() == true
        ensureUserDocument()
        fullSync()
        return if (existed && snapshot != null) profileFrom(snapshot.data, user.displayName) else null
    }

    suspend fun fetchRemoteProfile(): RemoteProfile? {
        val user = auth.currentUser ?: return null
        val snapshot = runCatching { users.document(user.uid).get().await() }.getOrNull()
            ?: return null
        if (!snapshot.exists()) return null
        return profileFrom(snapshot.data, user.displayName)
    }

    private fun profileFrom(raw: Map<String, Any>?, authName: String?): RemoteProfile {
        fun field(key: String): String? = (raw?.get(key) as? String)?.takeIf { it.isNotBlank() }
        return RemoteProfile(
            name = field("name") ?: field("displayName") ?: authName ?: "Operator",
            photoUrl = field("photoUrl"),
            joinedDate = field("joinedDate"),
            exists = true
        )
    }

    suspend fun signOut() {
        auth.signOut()
    }

    /** Creates the user document on first login and keeps profile/token fresh afterwards. */
    suspend fun ensureUserDocument() {
        val user = auth.currentUser ?: return
        val ref = users.document(user.uid)
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        val snapshot = runCatching { ref.get().await() }.getOrNull()

        if (snapshot == null || !snapshot.exists()) {
            // Brand-new user: seed from Auth once. Afterwards Firestore is authoritative —
            // never overwrite a custom in-app name/photo with the Auth profile on re-login.
            val authName = user.displayName?.takeIf { it.isNotBlank() } ?: "Operator"
            val data = HashMap<String, Any>()
            data["uid"] = user.uid
            data["name"] = authName
            data["displayName"] = authName
            data["photoUrl"] = user.photoUrl?.toString() ?: ""
            data["exp"] = LevelCalculator.INITIAL_EXP
            data["fcmToken"] = token ?: ""
            data["joinedDate"] = AccendDate.today().toIsoString()
            data["updatedAt"] = FieldValue.serverTimestamp()
            ref.set(data).await()
        } else {
            val update = HashMap<String, Any>()
            if (!token.isNullOrBlank()) update["fcmToken"] = token
            update["updatedAt"] = FieldValue.serverTimestamp()
            runCatching { ref.update(update).await() }
        }
    }

    suspend fun refreshFcmToken(token: String) {
        val uid = currentUid ?: return
        runCatching {
            users.document(uid).update("fcmToken", token).await()
        }
    }

    /** Publishes the in-app profile so the leaderboard uses it instead of the Auth profile. */
    suspend fun updateProfile(name: String, newPhotoUri: Uri?, removePhoto: Boolean) {
        val uid = currentUid ?: return
        val cleanName = name.trim().ifBlank { "Operator" }
        val update = HashMap<String, Any>()
        update["name"] = cleanName
        update["displayName"] = cleanName
        if (removePhoto) {
            update["photoUrl"] = ""
        } else if (newPhotoUri != null) {
            val imageRef = FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg")
            runCatching {
                imageRef.putFile(newPhotoUri).await()
                update["photoUrl"] = imageRef.downloadUrl.await().toString()
            }
        }
        update["updatedAt"] = FieldValue.serverTimestamp()
        runCatching { users.document(uid).update(update).await() }
    }

    /** Removes the account, all log subcollections, its stored avatar and the Auth user. */
    suspend fun deleteAccount(idToken: String?) {
        val user = auth.currentUser ?: return
        if (!idToken.isNullOrBlank()) {
            user.reauthenticate(GoogleAuthProvider.getCredential(idToken, null)).await()
        }
        val uid = user.uid
        while (true) {
            val docs = users.document(uid).collection("logs").limit(400).get().await().documents
            if (docs.isEmpty()) break
            val batch = firestore.batch()
            docs.forEach { batch.delete(it.reference) }
            batch.commit().await()
        }
        users.document(uid).delete().await()
        runCatching {
            FirebaseStorage.getInstance().reference.child("profile_images/$uid.jpg").delete().await()
        }
        user.delete().await()
    }

    suspend fun awardExp(amount: Int) {
        val uid = currentUid ?: return
        if (amount <= 0) return
        runCatching {
            firestore.runTransaction { tx ->
                val ref = users.document(uid)
                val snap = tx.get(ref)
                val current = (snap.get("exp") as? Number)?.toLong() ?: LevelCalculator.INITIAL_EXP
                tx.update(ref, mapOf("exp" to current + amount, "updatedAt" to FieldValue.serverTimestamp()))
                null
            }.await()
        }
    }

    fun observeMyExp(): Flow<Long> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(0L)
            awaitClose { }
            return@callbackFlow
        }
        val registration = users.document(uid).addSnapshotListener { snapshot, _ ->
            trySend((snapshot?.get("exp") as? Number)?.toLong() ?: LevelCalculator.INITIAL_EXP)
        }
        awaitClose { registration.remove() }
    }

    fun observeLeaderboard(): Flow<List<LeaderboardEntry>> = callbackFlow {
        val registration = users
            .orderBy("exp", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, _ ->
                val entries = snapshot?.documents.orEmpty().mapIndexed { index, doc ->
                    LeaderboardEntry(
                        uid = doc.id,
                        name = doc.getString("name")?.takeIf { it.isNotBlank() }
                            ?: doc.getString("displayName")?.takeIf { it.isNotBlank() }
                            ?: "OPERATOR",
                        photoUrl = doc.getString("photoUrl")?.takeIf { it.isNotBlank() },
                        exp = (doc.get("exp") as? Number)?.toLong() ?: 0L,
                        rank = index + 1
                    )
                }
                trySend(entries)
            }
        awaitClose { registration.remove() }
    }

    /** Pull remote logs down first, then push the merged local state back up. */
    suspend fun fullSync() {
        val uid = currentUid ?: return
        pullAll(uid)
        val today = AccendDate.today().toIsoString()
        pushDay(uid, today)
        // Also publish the last 30 days so a fresh install can recover history immediately.
        for (i in 1 until 30) pushDay(uid, AccendDate.today().minusDays(i).toIsoString())
    }

    suspend fun pushDayIfSignedIn(date: String) {
        val uid = currentUid ?: return
        runCatching { pushDay(uid, date) }
    }

    private suspend fun pushDay(uid: String, date: String) {
        val data = HashMap<String, Any>()

        val exercises = ArrayList<Map<String, Any>>()
        db.exerciseLogDao().forDateOnce(date).forEach { log ->
            val row = HashMap<String, Any>()
            row["exerciseId"] = log.exerciseId
            row["exerciseName"] = db.exerciseDao().getById(log.exerciseId)?.name ?: log.levelNameSnapshot
            row["done"] = log.done
            row["levelName"] = log.levelNameSnapshot
            row["volume"] = log.volumeSnapshot
            exercises.add(row)
        }
        data["exercise"] = exercises

        val nutrition = HashMap<String, Any>()
        db.nutritionDao().forDateOnce(date)?.let { n ->
            nutrition["protein"] = n.protein
            nutrition["heavyMeals"] = n.heavyMeals
            nutrition["breakfast"] = n.breakfast
            nutrition["hydration"] = n.hydration
            nutrition["sleep9h"] = n.sleep9h
        }
        data["nutrition"] = nutrition

        val mind = HashMap<String, Any>()
        val meditations = ArrayList<Map<String, Any>>()
        db.mindDao().meditationForDateOnce(date).forEach { m ->
            val row = HashMap<String, Any>()
            row["minutes"] = m.minutes
            row["restlessness"] = m.restlessness
            meditations.add(row)
        }
        val deepWork = ArrayList<Map<String, Any>>()
        db.mindDao().deepWorkForDateOnce(date).forEach { d ->
            val row = HashMap<String, Any>()
            row["minutes"] = d.minutes
            row["completed"] = d.completed
            deepWork.add(row)
        }
        mind["meditations"] = meditations
        mind["deepWork"] = deepWork
        db.mindDao().screenTimeForDateOnce(date)?.let { mind["screenTime"] = it.minutes }
        db.mindDao().readingForDateOnce(date)?.let { mind["reading"] = it.pages }
        data["mind"] = mind

        val money = HashMap<String, Any>()
        val transactions = ArrayList<Map<String, Any>>()
        db.moneyDao().allOnce().filter { it.date == date }.forEach { t ->
            val row = HashMap<String, Any>()
            row["type"] = t.type
            row["amount"] = t.amount
            row["category"] = t.category ?: ""
            row["source"] = t.source ?: ""
            row["note"] = t.note ?: ""
            row["impulseAnswer"] = t.impulseAnswer ?: ""
            transactions.add(row)
        }
        val savings = ArrayList<Map<String, Any>>()
        db.savingsDao().allOnce().filter { it.date == date }.forEach { s ->
            val row = HashMap<String, Any>()
            row["amount"] = s.amount
            row["direction"] = s.direction
            savings.add(row)
        }
        val skills = ArrayList<Map<String, Any>>()
        db.skillDao().allOnce().filter { it.date == date }.forEach { s ->
            val row = HashMap<String, Any>()
            row["hours"] = s.hours
            row["tag"] = s.tag
            skills.add(row)
        }
        money["transactions"] = transactions
        money["savings"] = savings
        money["skills"] = skills
        data["money"] = money
        data["updatedAt"] = FieldValue.serverTimestamp()

        // Never overwrite a remote day with an empty local snapshot (fresh reinstall).
        val mindMap = mind as Map<*, *>
        val moneyMap = money as Map<*, *>
        val hasLocalData = exercises.isNotEmpty() || nutrition.isNotEmpty() ||
            (mindMap["meditations"] as? List<*>)?.isNotEmpty() == true ||
            (mindMap["deepWork"] as? List<*>)?.isNotEmpty() == true ||
            mindMap["screenTime"] != null || mindMap["reading"] != null ||
            (moneyMap["transactions"] as? List<*>)?.isNotEmpty() == true ||
            (moneyMap["savings"] as? List<*>)?.isNotEmpty() == true ||
            (moneyMap["skills"] as? List<*>)?.isNotEmpty() == true
        if (!hasLocalData) return

        logDoc(uid, date).set(data).await()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun pullAll(uid: String) {
        val snapshot = runCatching {
            users.document(uid).collection("logs").get().await()
        }.getOrNull() ?: return

        val localExercises = db.exerciseDao().allOnce()
        for (doc in snapshot.documents) {
            val date = doc.id
            if (date.isBlank()) continue
            runCatching { pullDay(date, doc.data, localExercises) }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun pullDay(date: String, raw: Map<String, Any>?, localExercises: List<com.accend.app.data.db.ExerciseEntity>) {
        if (raw == null) return

        (raw["exercise"] as? List<*>)?.forEach { item ->
            val row = item as? Map<*, *> ?: return@forEach
            val name = (row["exerciseName"] as? String) ?: (row["levelName"] as? String) ?: return@forEach
            val exercise = localExercises.firstOrNull { it.name == name } ?: return@forEach
            val done = row["done"] as? Boolean ?: false
            val volume = (row["volume"] as? Number)?.toDouble() ?: 0.0
            db.exerciseLogDao().upsert(
                ExerciseLogEntity(
                    date = date,
                    exerciseId = exercise.id,
                    done = done,
                    levelNameSnapshot = (row["levelName"] as? String) ?: name,
                    volumeSnapshot = volume
                )
            )
        }

        (raw["nutrition"] as? Map<*, *>)?.let { n ->
            if (n.isNotEmpty() && db.nutritionDao().forDateOnce(date) == null) {
                db.nutritionDao().upsert(
                    NutritionLogEntity(
                        date = date,
                        protein = n["protein"] as? Boolean ?: false,
                        heavyMeals = n["heavyMeals"] as? Boolean ?: false,
                        breakfast = n["breakfast"] as? Boolean ?: false,
                        hydration = n["hydration"] as? Boolean ?: false,
                        sleep9h = n["sleep9h"] as? Boolean ?: false
                    )
                )
            }
        }

        (raw["mind"] as? Map<*, *>)?.let { mind ->
            val meds = mind["meditations"] as? List<*> ?: emptyList<Any>()
            if (db.mindDao().meditationForDateOnce(date).isEmpty()) {
                meds.forEach { item ->
                    val row = item as? Map<*, *> ?: return@forEach
                    db.mindDao().insertMeditation(
                        MeditationLogEntity(
                            date = date,
                            minutes = (row["minutes"] as? Number)?.toInt() ?: 1,
                            restlessness = (row["restlessness"] as? String) ?: "still"
                        )
                    )
                }
            }
            val deeps = mind["deepWork"] as? List<*> ?: emptyList<Any>()
            if (db.mindDao().deepWorkForDateOnce(date).isEmpty()) {
                deeps.forEach { item ->
                    val row = item as? Map<*, *> ?: return@forEach
                    db.mindDao().insertDeepWork(
                        DeepWorkLogEntity(
                            date = date,
                            minutes = (row["minutes"] as? Number)?.toInt() ?: 1,
                            completed = row["completed"] as? Boolean ?: false
                        )
                    )
                }
            }
            (mind["screenTime"] as? Number)?.let { minutes ->
                if (db.mindDao().screenTimeForDateOnce(date) == null) {
                    db.mindDao().upsertScreenTime(ScreenTimeLogEntity(date = date, minutes = minutes.toInt()))
                }
            }
            (mind["reading"] as? Number)?.let { pages ->
                if (db.mindDao().readingForDateOnce(date) == null) {
                    db.mindDao().upsertReading(ReadingLogEntity(date = date, pages = pages.toInt()))
                }
            }
        }

        (raw["money"] as? Map<*, *>)?.let { money ->
            val txs = money["transactions"] as? List<*> ?: emptyList<Any>()
            if (db.moneyDao().allOnce().none { it.date == date }) {
                txs.forEach { item ->
                    val row = item as? Map<*, *> ?: return@forEach
                    db.moneyDao().insert(
                        MoneyTransactionEntity(
                            date = date,
                            type = (row["type"] as? String) ?: "IN",
                            amount = (row["amount"] as? Number)?.toLong() ?: 0L,
                            category = row["category"] as? String,
                            source = row["source"] as? String,
                            note = row["note"] as? String,
                            impulseAnswer = row["impulseAnswer"] as? String
                        )
                    )
                }
            }
        }
    }
}
