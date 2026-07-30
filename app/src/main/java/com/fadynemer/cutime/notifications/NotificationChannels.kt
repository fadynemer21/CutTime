package com.fadynemer.cutime.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val APPOINTMENTS = "cutime_appointments"
    const val GENERAL = "cutime_general"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager =
            context.getSystemService(NotificationManager::class.java)
        val appointmentChannel = NotificationChannel(
            APPOINTMENTS,
            "Appointment updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description =
                "Booking confirmations, changes, cancellations, and reminders"
            enableVibration(true)
        }
        val generalChannel = NotificationChannel(
            GENERAL,
            "General updates",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "CuTime account and service updates"
        }

        manager.createNotificationChannels(
            listOf(appointmentChannel, generalChannel)
        )
    }
}
