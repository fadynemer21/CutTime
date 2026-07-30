package com.fadynemer.cutime.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberShopReadinessTest {
    @Test
    fun emptyReadinessIsNotBookable() {
        val readiness = BarberShopReadiness()

        assertFalse(readiness.isBookable)
        assertEquals(0, readiness.completedStepCount)
        assertEquals(3, readiness.totalStepCount)
    }

    @Test
    fun allRequirementsMakeShopBookable() {
        val readiness = BarberShopReadiness(
            profileComplete = true,
            validServiceCount = 2,
            availabilitySaved = true,
            hasOpenWorkingDay = true
        )

        assertTrue(readiness.profileComplete)
        assertTrue(readiness.servicesComplete)
        assertTrue(readiness.availabilityComplete)
        assertTrue(readiness.isBookable)
        assertEquals(3, readiness.completedStepCount)
    }

    @Test
    fun savedAvailabilityWithoutOpenDayIsIncomplete() {
        val readiness = BarberShopReadiness(
            profileComplete = true,
            validServiceCount = 1,
            availabilitySaved = true,
            hasOpenWorkingDay = false
        )

        assertFalse(readiness.availabilityComplete)
        assertFalse(readiness.isBookable)
        assertEquals(2, readiness.completedStepCount)
    }

    @Test
    fun profileRequiresNameAndMeaningfulDescription() {
        assertFalse(
            BarberShopReadinessEvaluator.profileComplete(
                "",
                "A complete description"
            )
        )
        assertFalse(
            BarberShopReadinessEvaluator.profileComplete(
                "A",
                "A complete description"
            )
        )
        assertFalse(
            BarberShopReadinessEvaluator.profileComplete(
                "Studio",
                "Too short"
            )
        )
        assertTrue(
            BarberShopReadinessEvaluator.profileComplete(
                "Studio",
                "Professional cuts and beard styling."
            )
        )
    }

    @Test
    fun profileTrimsValuesBeforeValidation() {
        assertTrue(
            BarberShopReadinessEvaluator.profileComplete(
                "  AB  ",
                "  A long enough description.  "
            )
        )
    }

    @Test
    fun validServicesAreCounted() {
        val services = listOf(
            service("Haircut", 60, 30),
            service("Beard", 35, 15),
            service("Invalid price", 0, 30),
            service("Invalid duration", 50, 20),
            service("X", 50, 30)
        )

        assertEquals(
            2,
            BarberShopReadinessEvaluator.validServiceCount(
                services
            )
        )
    }

    @Test
    fun numericFirestoreTypesAreAcceptedForServices() {
        val service = mapOf<String, Any?>(
            "name" to "Haircut",
            "price" to 60L,
            "durationMinutes" to 30L
        )

        assertEquals(
            1,
            BarberShopReadinessEvaluator.validServiceCount(
                listOf(service)
            )
        )
    }

    @Test
    fun availabilityRequiresPersistedSevenDayWeek() {
        val sixDays = List(6) { index ->
            day("Day $index", isOpen = false)
        }

        assertEquals(
            false to false,
            BarberShopReadinessEvaluator.availabilityState(
                exists = true,
                rawDays = sixDays
            )
        )
        assertEquals(
            false to false,
            BarberShopReadinessEvaluator.availabilityState(
                exists = false,
                rawDays = validWeek()
            )
        )
    }

    @Test
    fun allClosedWeekIsSavedButNotBookable() {
        val result =
            BarberShopReadinessEvaluator.availabilityState(
                exists = true,
                rawDays =
                    validWeek().map {
                        it + ("isOpen" to false)
                    }
            )

        assertEquals(true to false, result)
    }

    @Test
    fun oneOpenDayMakesAvailabilityComplete() {
        val result =
            BarberShopReadinessEvaluator.availabilityState(
                exists = true,
                rawDays = validWeek()
            )

        assertEquals(true to true, result)
    }

    @Test
    fun invalidOpenIntervalRejectsAvailabilityDocument() {
        val days = validWeek().toMutableList()
        days[0] = day(
            name = "Sunday",
            isOpen = true,
            start = "17:00",
            end = "09:00"
        )

        assertEquals(
            false to false,
            BarberShopReadinessEvaluator.availabilityState(
                exists = true,
                rawDays = days
            )
        )
    }

    @Test
    fun invalidClockValueRejectsAvailabilityDocument() {
        val days = validWeek().toMutableList()
        days[0] = day(
            name = "Sunday",
            isOpen = true,
            start = "25:00",
            end = "26:00"
        )

        assertEquals(
            false to false,
            BarberShopReadinessEvaluator.availabilityState(
                exists = true,
                rawDays = days
            )
        )
    }

    private fun service(
        name: String,
        price: Int,
        duration: Int
    ) = mapOf<String, Any?>(
        "name" to name,
        "price" to price,
        "durationMinutes" to duration
    )

    private fun validWeek(): List<Map<String, Any?>> {
        return listOf(
            day("Sunday", true),
            day("Monday", true),
            day("Tuesday", true),
            day("Wednesday", true),
            day("Thursday", true),
            day("Friday", true, "09:00", "14:00"),
            day("Saturday", false)
        )
    }

    private fun day(
        name: String,
        isOpen: Boolean,
        start: String = "09:00",
        end: String = "19:00"
    ) = mapOf<String, Any?>(
        "day" to name,
        "isOpen" to isOpen,
        "startTime" to start,
        "endTime" to end
    )
}
