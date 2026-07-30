package com.fadynemer.cutime.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.NotificationPreferences
import com.fadynemer.cutime.model.NotificationPreferenceLimits
import java.time.Clock
import java.util.concurrent.TimeUnit

object AppointmentReminderScheduler {
    fun sync(
        context: Context,
        appointments: List<Appointment>,
        preferences: NotificationPreferences,
        clock: Clock = Clock.systemDefaultZone()
    ) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(
            AppointmentReminderWorker.WORK_TAG
        )

        if (
            !preferences.pushEnabled ||
            !preferences.remindersEnabled
        ) {
            return
        }

        appointments.forEach { appointment ->
            ReminderPlanner.plans(
                appointment = appointment,
                clock = clock
            ).forEach { plan ->
                val input = Data.Builder()
                    .putString(
                        AppointmentReminderWorker
                            .KEY_APPOINTMENT_ID,
                        appointment.id
                    )
                    .putString(
                        AppointmentReminderWorker.KEY_TITLE,
                        plan.title
                    )
                    .putString(
                        AppointmentReminderWorker.KEY_MESSAGE,
                        plan.message
                    )
                    .putInt(
                        AppointmentReminderWorker
                            .KEY_LEAD_MINUTES,
                        plan.leadMinutes
                    )
                    .build()
                val request =
                    OneTimeWorkRequestBuilder<
                        AppointmentReminderWorker
                        >()
                        .setInitialDelay(
                            plan.delayMillis,
                            TimeUnit.MILLISECONDS
                        )
                        .setInputData(input)
                        .addTag(
                            AppointmentReminderWorker.WORK_TAG
                        )
                        .build()

                workManager.enqueueUniqueWork(
                    AppointmentReminderWorker.uniqueWorkName(
                        appointmentId = appointment.id,
                        leadMinutes = plan.leadMinutes
                    ),
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
        }
    }

    fun cancel(
        context: Context,
        appointmentId: String
    ) {
        val workManager = WorkManager.getInstance(context)
        NotificationPreferenceLimits.REMINDER_LEAD_MINUTES
            .forEach { leadMinutes ->
                workManager.cancelUniqueWork(
                    AppointmentReminderWorker.uniqueWorkName(
                        appointmentId,
                        leadMinutes
                    )
                )
            }
    }
}
