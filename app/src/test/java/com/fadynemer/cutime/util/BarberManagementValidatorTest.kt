package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.DayAvailability
import com.fadynemer.cutime.model.ManagedBarberProfile
import com.fadynemer.cutime.model.WorkingPeriod
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

    @Test
    fun overlappingWorkingPeriods_areRejected() {
        val availability = BarberAvailability(
            days = listOf(
                DayAvailability(
                    day = "Wednesday",
                    isOpen = true,
                    startTime = "09:00",
                    endTime = "12:00",
                    workingPeriods = listOf(
                        WorkingPeriod("09:00", "12:00"),
                        WorkingPeriod("11:30", "16:00")
                    )
                )
            )
        )

        assertTrue(
            BarberManagementValidator.validateAvailability(availability)
                ?.contains("cannot overlap") == true
        )
    }

    @Test
    fun appointmentsMustFitWithinOnePeriodAndCannotCrossBreak() {
        val wednesday = LocalDate.parse("2030-01-09")
        val availability = BarberAvailability(
            days = listOf(
                DayAvailability(
                    day = "Wednesday",
                    isOpen = true,
                    startTime = "09:00",
                    endTime = "12:00",
                    workingPeriods = listOf(
                        WorkingPeriod("09:00", "12:00"),
                        WorkingPeriod("14:00", "16:00"),
                        WorkingPeriod("16:30", "19:00")
                    )
                )
            )
        )

        assertTrue(
            BarberManagementValidator.isBookable(
                availability,
                wednesday,
                LocalTime.of(14, 30),
                60
            )
        )
        assertFalse(
            BarberManagementValidator.isBookable(
                availability,
                wednesday,
                LocalTime.of(11, 45),
                30
            )
        )
        assertFalse(
            BarberManagementValidator.isBookable(
                availability,
                wednesday,
                LocalTime.of(16, 0),
                30
            )
        )
    }

    @Test
    fun appointmentCannotWrapPastMidnight() {
        val monday = LocalDate.parse("2030-01-07")
        val availability = BarberAvailability(
            days = listOf(
                DayAvailability(
                    day = "Monday",
                    isOpen = true,
                    startTime = "23:00",
                    endTime = "23:59"
                )
            )
        )

        assertFalse(
            BarberManagementValidator.isBookable(
                availability,
                monday,
                LocalTime.of(23, 45),
                30
            )
        )
    }
}
