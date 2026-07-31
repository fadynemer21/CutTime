package com.fadynemer.cutime.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.fadynemer.cutime.MainActivity
import com.fadynemer.cutime.R
import com.fadynemer.cutime.navigation.AppRoutePolicy

object SystemNotificationPublisher {
    const val EXTRA_ROUTE = "notification_route"

    fun publish(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        route: String?,
        channelId: String = NotificationChannels.APPOINTMENTS
    ): Boolean {
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }

        NotificationChannels.create(context)
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(
                    EXTRA_ROUTE,
                    route?.takeIf(AppRoutePolicy::isAllowed)
                )
            }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
        val notification =
            NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(
                    title.ifBlank {
                        context.getString(
                            R.string.notification_default_title
                        )
                    }
                )
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(message)
                )
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

        NotificationManagerCompat.from(context)
            .notify(notificationId, notification)
        return true
    }

    fun stableId(seed: String): Int {
        return seed.hashCode() and Int.MAX_VALUE
    }
}
