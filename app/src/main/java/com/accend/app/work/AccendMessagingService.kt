package com.accend.app.work

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Receives server-sent alerts and keeps the FCM token in sync with the user document. */
class AccendMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: message.data["title"] ?: "ACCEND"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        if (body.isNotBlank()) Notifications.postFcmMessage(applicationContext, title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val app = applicationContext as? com.accend.app.AccendApp ?: return
        CoroutineScope(Dispatchers.IO).launch {
            app.container.cloudRepo.refreshFcmToken(token)
        }
    }
}
