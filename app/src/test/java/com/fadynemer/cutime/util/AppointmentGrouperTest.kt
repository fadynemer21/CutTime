package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class AppointmentGrouperTest {

    @Test
    fun appointments_areGroupedAndSorted() {
        val now = 1_000_000L
        val appointments = listOf(
            appointment(
                id = "future_later",
                startAt = now + 30_000L,
                endAt = now + 40_000L
            ),
            appointment(
                id = "past",
                startAt = now - 30_000L,
                endAt = now - 20_000L
            ),
            appointment(
                id = "cancelled",
                startAt = now + 10_000L,
                endAt = now + 20_000L,
                status = AppointmentStatus.CANCELLED
            ),
            appointment(
                id = "future_first",
                startAt = now + 10_000L,
                endAt = now + 20_000L
            )
        )

        val groups =
            AppointmentGrouper.group(
                appointments = appointments,
                nowMillis = now
            )

        assertEquals(
            listOf("future_first", "future_later"),
            groups.upcoming.map { it.id }
        )
        assertEquals(
            listOf("past"),
            groups.completed.map { it.id }
        )
        assertEquals(
            listOf("cancelled"),
            groups.cancelled.map { it.id }
        )
    }

    private fun appointment(
        id: String,
        startAt: Long,
        endAt: Long,
        status: AppointmentStatus = AppointmentStatus.UPCOMING
    ) = Appointment(
        id = id,
        customerId = "customer_1",
        barberId = "barber_1",
        barberName = "Urban Fade Studio",
        serviceId = "service_1",
        serviceName = "Classic Haircut",
        price = 60,
        durationMinutes = 30,
        appointmentDate = "2026-07-30",
        appointmentTime = "14:30",
        startAtMillis = startAt,
        endAtMillis = endAt,
        status = status
    )
}
