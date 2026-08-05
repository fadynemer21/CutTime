package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.DayAvailability
import com.fadynemer.cutime.model.WorkingPeriod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class AvailabilitySlotGeneratorTest {
    private val zone = ZoneId.of("Asia/Jerusalem")
    private val monday = LocalDate.parse("2030-01-07")
    private val clock = Clock.fixed(
        Instant.parse("2030-01-06T10:00:00Z"),
        zone
    )

    @Test
    fun thirtyMinuteService_generatesQuarterHourStarts() {
        val slots = AvailabilitySlotGenerator.availableTimes(
            availability = availability(
                start = "09:00",
                end = "10:00"
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertEquals(
            listOf("09:00", "09:15", "09:30"),
            slots
        )
    }

    @Test
    fun serviceCannotEndAfterClosingTime() {
        val slots = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "10:00"
            ),
            date = monday,
            durationMinutes = 45,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertEquals(listOf("09:00", "09:15"), slots.map { it.time })
    }

    @Test
    fun occupiedMiddleSegment_blocksEveryOverlappingStart() {
        val available = AvailabilitySlotGenerator.availableTimes(
            availability = availability(
                start = "09:00",
                end = "10:30"
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = setOf("09:30"),
            clock = clock
        )

        assertFalse("09:15" in available)
        assertFalse("09:30" in available)
        assertTrue("09:00" in available)
        assertTrue("09:45" in available)
    }

    @Test
    fun occupiedFirstSegment_blocksExactStart() {
        val generated = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "10:00"
            ),
            date = monday,
            durationMinutes = 15,
            occupiedTimes = setOf("09:15"),
            clock = clock
        )

        assertTrue(generated.first { it.time == "09:00" }.isAvailable)
        assertFalse(generated.first { it.time == "09:15" }.isAvailable)
        assertTrue(generated.first { it.time == "09:30" }.isAvailable)
    }

    @Test
    fun blockedDate_hasNoSlots() {
        val result = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "17:00",
                blockedDates = listOf(monday.toString())
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun closedDay_hasNoSlots() {
        val result = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "17:00",
                isOpen = false
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun missingWorkingDay_hasNoSlots() {
        val result = AvailabilitySlotGenerator.generate(
            availability = BarberAvailability(
                days = listOf(
                    DayAvailability(
                        day = "Tuesday",
                        isOpen = true,
                        startTime = "09:00",
                        endTime = "17:00"
                    )
                )
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun invalidOpeningTime_hasNoSlots() {
        val result = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "morning",
                end = "17:00"
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun invalidClosingTime_hasNoSlots() {
        val result = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "evening"
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun zeroDuration_hasNoSlots() {
        val result = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "17:00"
            ),
            date = monday,
            durationMinutes = 0,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun negativeDuration_hasNoSlots() {
        val result = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "17:00"
            ),
            date = monday,
            durationMinutes = -15,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun durationThatExceedsWorkingDay_hasNoSlots() {
        val result = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "09:30"
            ),
            date = monday,
            durationMinutes = 45,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun generatedSlot_exposesAllReservedSegments() {
        val first = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "10:00"
            ),
            date = monday,
            durationMinutes = 45,
            occupiedTimes = emptySet(),
            clock = clock
        ).first()

        assertEquals(
            listOf("09:00", "09:15", "09:30"),
            first.occupiedSegments
        )
    }

    @Test
    fun nonMultipleDuration_roundsReservationUpToNextSegment() {
        val first = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "10:00"
            ),
            date = monday,
            durationMinutes = 20,
            occupiedTimes = emptySet(),
            clock = clock
        ).first()

        assertEquals(
            listOf("09:00", "09:15"),
            first.occupiedSegments
        )
    }

    @Test
    fun sameDayPastAndCurrentStarts_areUnavailable() {
        val sameDayClock = Clock.fixed(
            Instant.parse("2030-01-07T08:30:00Z"),
            zone
        )
        val generated = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "13:00"
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = sameDayClock
        )

        assertTrue(
            generated
                .filter { it.time <= "10:30" }
                .all { !it.isAvailable }
        )
        assertTrue(
            generated.first { it.time == "10:45" }.isAvailable
        )
    }

    @Test
    fun pastDate_hasNoAvailableSlotsButRetainsScheduleShape() {
        val afterMondayClock = Clock.fixed(
            Instant.parse("2030-01-08T10:00:00Z"),
            zone
        )
        val generated = AvailabilitySlotGenerator.generate(
            availability = availability(
                start = "09:00",
                end = "10:00"
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = afterMondayClock
        )

        assertTrue(generated.isNotEmpty())
        assertTrue(generated.none { it.isAvailable })
    }

    @Test
    fun dayMatching_isCaseInsensitive() {
        val result = AvailabilitySlotGenerator.availableTimes(
            availability = BarberAvailability(
                days = listOf(
                    DayAvailability(
                        day = "mOnDaY",
                        isOpen = true,
                        startTime = "09:00",
                        endTime = "10:00"
                    )
                )
            ),
            date = monday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertEquals(
            listOf("09:00", "09:15", "09:30"),
            result
        )
    }

    @Test
    fun multipleWorkingPeriods_excludeBreaksFromGeneratedSlots() {
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

        val slots = AvailabilitySlotGenerator.availableTimes(
            availability = availability,
            date = wednesday,
            durationMinutes = 30,
            occupiedTimes = emptySet(),
            clock = clock
        )

        assertTrue("11:30" in slots)
        assertFalse("11:45" in slots)
        assertFalse("12:00" in slots)
        assertFalse("13:45" in slots)
        assertTrue("14:00" in slots)
        assertTrue("15:30" in slots)
        assertFalse("16:00" in slots)
        assertTrue("16:30" in slots)
        assertTrue("18:30" in slots)
    }

    @Test
    fun nextAvailabilityUsesOnlyFutureSlotsToday() {
        val eveningClock = Clock.fixed(
            Instant.parse("2030-01-07T17:00:00Z"),
            zone
        )
        val result = NextAvailabilityFormatter.format(
            availability = BarberAvailability(
                days = listOf(
                    DayAvailability(
                        day = "Monday",
                        isOpen = true,
                        startTime = "09:00",
                        endTime = "20:00"
                    )
                )
            ),
            durationMinutes = 30,
            occupiedTimesByDate = emptyMap(),
            clock = eveningClock
        )

        assertEquals("Available today", result)
    }

    @Test
    fun nextAvailabilityMovesToNearestOpenWeekdayAfterClosing() {
        val afterClosingClock = Clock.fixed(
            Instant.parse("2030-01-07T17:00:00Z"),
            zone
        )
        val result = NextAvailabilityFormatter.format(
            availability = BarberAvailability(
                days = listOf(
                    DayAvailability(
                        day = "Monday",
                        isOpen = true,
                        startTime = "09:00",
                        endTime = "19:00"
                    ),
                    DayAvailability(
                        day = "Wednesday",
                        isOpen = true,
                        startTime = "09:00",
                        endTime = "10:00"
                    )
                )
            ),
            durationMinutes = 30,
            occupiedTimesByDate = emptyMap(),
            clock = afterClosingClock
        )

        assertEquals("Available Wednesday", result)
    }

    @Test
    fun nextAvailabilitySkipsFullyBookedDay() {
        val mondayMorningClock = Clock.fixed(
            Instant.parse("2030-01-07T06:00:00Z"),
            zone
        )
        val result = NextAvailabilityFormatter.format(
            availability = BarberAvailability(
                days = listOf(
                    DayAvailability(
                        day = "Monday",
                        isOpen = true,
                        startTime = "09:00",
                        endTime = "10:00"
                    ),
                    DayAvailability(
                        day = "Tuesday",
                        isOpen = true,
                        startTime = "09:00",
                        endTime = "10:00"
                    )
                )
            ),
            durationMinutes = 30,
            occupiedTimesByDate = mapOf(
                "2030-01-07" to
                    setOf("09:00", "09:15", "09:30", "09:45")
            ),
            clock = mondayMorningClock
        )

        assertEquals("Available tomorrow", result)
    }

    private fun availability(
        start: String,
        end: String,
        isOpen: Boolean = true,
        blockedDates: List<String> = emptyList()
    ): BarberAvailability {
        return BarberAvailability(
            days = listOf(
                DayAvailability(
                    day = "Monday",
                    isOpen = isOpen,
                    startTime = start,
                    endTime = end
                )
            ),
            blockedDates = blockedDates
        )
    }
}
