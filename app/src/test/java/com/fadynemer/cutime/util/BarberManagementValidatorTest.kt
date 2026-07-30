package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.DayAvailability
import com.fadynemer.cutime.model.ManagedBarberProfile
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberManagementValidatorTest {

    @Test
    fun completeProfile_isValid() {
        assertNull(
            BarberManagementValidator.validateProfile(
                ManagedBarberProfile(
                    shopName = "CutTime Studio",
                    description =
                        "Modern haircuts and beard styling."
                )
            )
        )
    }

    @Test
    fun shortProfileDescription_isRejected() {
        assertTrue(
            BarberManagementValidator.validateProfile(
                ManagedBarberProfile(
                    shopName = "Studio",
                    description = "Short"
                )
            )?.contains("10") == true
        )
    }

    @Test
    fun serviceRequiresQuarterHourDuration() {
        val error =
            BarberManagementValidator.validateService(
                BarberService(
                    id = "",
                    name = "Haircut",
                    price = 60,
                    durationMinutes = 40
                )
            )

        assertTrue(error?.contains("15-minute") == true)
    }

    @Test
    fun closingBeforeOpening_isRejected() {
        val availability = BarberAvailability(
            days = listOf(
                DayAvailability(
                    day = "Sunday",
                    isOpen = true,
                    startTime = "18:00",
                    endTime = "09:00"
                )
            )
        )

        assertTrue(
            BarberManagementValidator
                .validateAvailability(availability)
                ?.contains("closing time") == true
        )
    }

    @Test
    fun blockedDate_isNotBookable() {
        val date = LocalDate.parse("2026-08-15")
        val availability = BarberAvailability(
            blockedDates = listOf(date.toString())
        )

        assertFalse(
            BarberManagementValidator.isBookable(
                availability = availability,
                date = date,
                time = LocalTime.of(10, 0),
                durationMinutes = 30
            )
        )
    }

    @Test
    fun appointmentMustFinishInsideWorkingHours() {
        val friday = LocalDate.parse("2026-07-31")
        val availability = BarberAvailability()

        assertTrue(
            BarberManagementValidator.isBookable(
                availability = availability,
                date = friday,
                time = LocalTime.of(13, 30),
                durationMinutes = 30
            )
        )
        assertFalse(
            BarberManagementValidator.isBookable(
                availability = availability,
                date = friday,
                time = LocalTime.of(13, 45),
                durationMinutes = 30
            )
        )
    }
}
