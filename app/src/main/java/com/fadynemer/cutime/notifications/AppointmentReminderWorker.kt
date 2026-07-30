package com.fadynemer.cutime.notifications

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.fadynemer.cutime.navigation.AppRoute

class AppointmentReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val appointmentId =
            inputData.getString(KEY_APPOINTMENT_ID)
                ?: return Result.failure()
        val title =
            inputData.getString(KEY_TITLE)
                ?: "Upcoming appointment"
        val message =
            inputData.getString(KEY_MESSAGE)
                ?: return Result.failure()
        val leadMinutes =
            inputData.getInt(KEY_LEAD_MINUTES, 0)
                .takeIf { it > 0 }
                ?: return Result.failure()

        val published =
            SystemNotificationPublisher.publish(
                context = applicationContext,
                notificationId =
                    SystemNotificationPublisher.stableId(
                        "reminder:$appointmentId:$leadMinutes"
                    ),
                title = title,
                message = message,
                route =
                    AppRoute.CustomerAppointmentDetail.create(
                        appointmentId
                    )
            )

        return if (published) {
            Result.success()
        } else {
            // Permission refusal is not transient. Retrying would waste
            // battery and still not display a notification.
            Result.failure()
        }
    }

    companion object {
        const val KEY_APPOINTMENT_ID = "appointmentId"
        const val KEY_TITLE = "title"
        const val KEY_MESSAGE = "message"
        const val KEY_LEAD_MINUTES = "leadMinutes"
        const val WORK_TAG = "cutime_appointment_reminders"
        const val WORK_PREFIX = "cutime_reminder_"

        fun uniqueWorkName(
            appointmentId: String,
            leadMinutes: Int
        ): String {
            return "$WORK_PREFIX${appointmentId}_$leadMinutes"
        }
    }
}
