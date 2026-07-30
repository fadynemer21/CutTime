package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.AppointmentStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HolidayCancellationPolicyTest {

    @Test
    fun upcomingAppointmentOnBlockedDate_isCancelled() {
        assertTrue(
            HolidayCancellationPolicy.shouldCancel(
                status = AppointmentStatus.UPCOMING.name,
                appointmentDate = "2026-08-12",
                blockedDates = setOf("2026-08-12")
            )
        )
    }

    @Test
    fun completedOrDifferentDate_isNotCancelled() {
        assertFalse(
            HolidayCancellationPolicy.shouldCancel(
                status = AppointmentStatus.COMPLETED.name,
                appointmentDate = "2026-08-12",
                blockedDates = setOf("2026-08-12")
            )
        )
        assertFalse(
            HolidayCancellationPolicy.shouldCancel(
                status = AppointmentStatus.UPCOMING.name,
                appointmentDate = "2026-08-13",
                blockedDates = setOf("2026-08-12")
            )
        )
    }

    @Test
    fun batchWriteCount_includesAvailabilityAppointmentsAndSlots() {
        assertEquals(
            10,
            HolidayCancellationPolicy.requiredBatchWrites(
                affectedSlotCounts = listOf(2, 4),
                baseWrites = 2
            )
        )
    }
}
