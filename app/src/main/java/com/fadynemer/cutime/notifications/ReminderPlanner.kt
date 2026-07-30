package com.fadynemer.cutime.notifications

import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.model.NotificationPreferenceLimits
import java.time.Clock

data class ReminderPlan(
    val appointmentId: String,
    val leadMinutes: Int,
    val triggerAtMillis: Long,
    val delayMillis: Long,
    val title: String,
    val message: String
)

object ReminderPlanner {
    fun plans(
        appointment: Appointment,
        clock: Clock = Clock.systemDefaultZone()
    ): List<ReminderPlan> {
        return NotificationPreferenceLimits
            .REMINDER_LEAD_MINUTES
            .mapNotNull { minutesBefore ->
                plan(
                    appointment = appointment,
                    minutesBefore = minutesBefore,
                    clock = clock
                )
            }
    }

    fun plan(
        appointment: Appointment,
        minutesBefore: Int,
        clock: Clock = Clock.systemDefaultZone()
    ): ReminderPlan? {
        if (
            appointment.status != AppointmentStatus.UPCOMING ||
            minutesBefore <= 0
        ) {
            return null
        }

        val triggerAt =
            appointment.startAtMillis -
                minutesBefore * 60_000L
        val delay = triggerAt - clock.millis()

        if (delay <= 0L) {
            return null
        }

        return ReminderPlan(
            appointmentId = appointment.id,
            leadMinutes = minutesBefore,
            triggerAtMillis = triggerAt,
            delayMillis = delay,
            title = "Upcoming appointment",
            message =
                "${appointment.serviceName} with ${appointment.barberName} starts in ${formatLeadTime(minutesBefore)}."
        )
    }

    fun formatLeadTime(minutes: Int): String {
        return when {
            minutes < 60 -> "$minutes minutes"
            minutes == 60 -> "1 hour"
            minutes % 60 == 0 ->
                "${minutes / 60} hours"
            else ->
                "${minutes / 60}h ${minutes % 60}m"
        }
    }
}
