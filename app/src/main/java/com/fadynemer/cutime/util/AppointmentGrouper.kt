package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentGroups
import com.fadynemer.cutime.model.AppointmentStatus

object AppointmentGrouper {

    fun group(
        appointments: List<Appointment>,
        nowMillis: Long = System.currentTimeMillis()
    ): AppointmentGroups {
        val cancelled = appointments
            .filter { appointment ->
                appointment.status == AppointmentStatus.CANCELLED
            }
            .sortedByDescending { appointment ->
                appointment.startAtMillis
            }

        val completed = appointments
            .filter { appointment ->
                appointment.status == AppointmentStatus.COMPLETED ||
                    (
                        appointment.status == AppointmentStatus.UPCOMING &&
                            appointment.endAtMillis <= nowMillis
                        )
            }
            .sortedByDescending { appointment ->
                appointment.startAtMillis
            }

        val upcoming = appointments
            .filter { appointment ->
                appointment.status == AppointmentStatus.UPCOMING &&
                    appointment.endAtMillis > nowMillis
            }
            .sortedBy { appointment ->
                appointment.startAtMillis
            }

        return AppointmentGroups(
            upcoming = upcoming,
            completed = completed,
            cancelled = cancelled
        )
    }
}
