package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.DayAvailability
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
