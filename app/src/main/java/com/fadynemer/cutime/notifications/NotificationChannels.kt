package com.fadynemer.cutime.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.fadynemer.cutime.R

object NotificationChannels {
    const val APPOINTMENTS = "cutime_appointments"
    const val GENERAL = "cutime_general"

    fun create(context: Context) {
        val manager =
            context.getSystemService(NotificationManager::class.java)
        val appointmentChannel = NotificationChannel(
            APPOINTMENTS,
            context.getString(
                R.string.notification_channel_appointments
            ),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                context.getString(
                    R.string
                        .notification_channel_appointments_description
                )
            enableVibration(true)
        }
        val generalChannel = NotificationChannel(
            GENERAL,
            context.getString(R.string.notification_channel_general),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(
                R.string.notification_channel_general_description
            )
        }

        manager.createNotificationChannels(
            listOf(appointmentChannel, generalChannel)
        )
    }
}
