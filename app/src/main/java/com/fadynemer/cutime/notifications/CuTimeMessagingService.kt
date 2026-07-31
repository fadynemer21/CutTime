package com.fadynemer.cutime.notifications

import com.fadynemer.cutime.repository.NotificationRepository
import com.fadynemer.cutime.util.NotificationRouter
import com.fadynemer.cutime.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
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
                ?: getString(R.string.notification_default_title)
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
                    ?.takeIf {
                        it == NotificationChannels.APPOINTMENTS ||
                            it == NotificationChannels.GENERAL
                    }
                    ?: NotificationChannels.APPOINTMENTS
        )
    }
}
