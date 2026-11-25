package com.accend.app.ui.signin

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.accend.app.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

/** Shared Google ID token acquisition for sign-in and sensitive-account reauthentication. */
object GoogleSignInHelper {

    suspend fun getIdToken(context: Context): String {
        val manager = CredentialManager.create(context)
        // default_web_client_id is the Web OAuth client from google-services.json.
        val serverClientId = context.getString(R.string.default_web_client_id)
        return try {
            val option = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .build()
            tokenFrom(manager, context, option)
        } catch (e: NoCredentialException) {
            // Fresh install / no cached credential: force the explicit account picker.
            val option = GetSignInWithGoogleOption.Builder(serverClientId).build()
            tokenFrom(manager, context, option)
        }
    }

    private suspend fun tokenFrom(
        manager: CredentialManager,
        context: Context,
        option: CredentialOption
    ): String {
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val result = manager.getCredential(context, request)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            return GoogleIdTokenCredential.createFrom(credential.data).idToken
        }
        error("Unsupported Google credential.")
    }
}
