package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BookingRequest
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentDateTimeTest {

    private val clock =
        Clock.fixed(
            Instant.parse("2026-07-30T09:00:00Z"),
            ZoneId.of("Asia/Jerusalem")
        )

    @Test
    fun futureRequest_isValid() {
        val request = bookingRequest(
            date = "2026-07-30",
            time = "14:30"
        )

        assertNull(
            AppointmentDateTime.validate(request, clock)
        )
    }

    @Test
    fun pastRequest_isRejected() {
        val request = bookingRequest(
            date = "2026-07-30",
            time = "10:00"
        )

        assertFalse(
            AppointmentDateTime.isFuture(
                request.appointmentDate,
                request.appointmentTime,
                clock
            )
        )
        assertTrue(
            AppointmentDateTime.validate(request, clock)
                ?.contains("future") == true
        )
    }

    @Test
    fun sixtyMinuteService_reservesFourQuarterHourSlots() {
        val slots =
            AppointmentDateTime.slotDocumentIds(
                barberId = "barber_1",
                date = "2026-07-30",
                time = "14:30",
                durationMinutes = 60
            )

        assertEquals(
            listOf(
                "barber_1_2026-07-30_14-30",
                "barber_1_2026-07-30_14-45",
                "barber_1_2026-07-30_15-00",
                "barber_1_2026-07-30_15-15"
            ),
            slots
        )
    }

    @Test
    fun reservedTimes_matchSlotDocumentSegments() {
        assertEquals(
            listOf("14:30", "14:45", "15:00", "15:15"),
            AppointmentDateTime.reservedTimes(
                time = "14:30",
                durationMinutes = 60
            )
        )
    }

    @Test
    fun nonMultipleDuration_reservesPartialFinalSegment() {
        assertEquals(
            listOf("14:30", "14:45"),
            AppointmentDateTime.reservedTimes(
                time = "14:30",
                durationMinutes = 20
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun reservedTimes_rejectsZeroDuration() {
        AppointmentDateTime.reservedTimes(
            time = "14:30",
            durationMinutes = 0
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun reservedTimes_rejectsZeroInterval() {
        AppointmentDateTime.reservedTimes(
            time = "14:30",
            durationMinutes = 30,
            intervalMinutes = 0
        )
    }

    @Test
    fun isoDate_formatsInEnglish() {
        assertEquals(
            "Thursday, 30 July 2026",
            AppointmentDateTime.formatDateForDisplay(
                "2026-07-30"
            )
        )
    }

    private fun bookingRequest(
        date: String,
        time: String
    ) = BookingRequest(
        barberId = "barber_1",
        barberName = "Urban Fade Studio",
        serviceId = "service_1",
        serviceName = "Classic Haircut",
        price = 60,
        durationMinutes = 30,
        appointmentDate = date,
        appointmentTime = time
    )
}
