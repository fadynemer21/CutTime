package com.fadynemer.cutime.notifications

import com.fadynemer.cutime.repository.NotificationRepository
import com.fadynemer.cutime.util.NotificationRouter
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class CuTimeMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        NotificationRegistrationManager.registerToken(
            context = applicationContext,
            token = token,
            repository = NotificationRepository()
        )
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data
        val title =
            message.notification?.title
                ?: data["title"]
                ?: "CuTime"
        val body =
            message.notification?.body
                ?: data["body"]
                ?: return
        val route =
            NotificationRouter.destinationFromPayload(data)
        val stableSeed =
            message.messageId
                ?: data["notificationId"]
                ?: "$title:$body"

        SystemNotificationPublisher.publish(
            context = applicationContext,
            notificationId =
                SystemNotificationPublisher.stableId(stableSeed),
            title = title,
            message = body,
            route = route,
            channelId =
                data["channelId"]
                    ?: NotificationChannels.APPOINTMENTS
        )
    }
}
