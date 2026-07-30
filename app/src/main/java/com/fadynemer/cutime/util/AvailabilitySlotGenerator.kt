package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.DayAvailability
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale

data class GeneratedSlot(
    val time: String,
    val occupiedSegments: List<String>,
    val isAvailable: Boolean
)

object AvailabilitySlotGenerator {
    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)

    fun generate(
        availability: BarberAvailability,
        date: LocalDate,
        durationMinutes: Int,
        occupiedTimes: Set<String>,
        clock: Clock = Clock.systemDefaultZone(),
        intervalMinutes: Int = 15
    ): List<GeneratedSlot> {
        if (
            durationMinutes <= 0 ||
            intervalMinutes <= 0 ||
            date.toString() in availability.blockedDates
        ) {
            return emptyList()
        }

        val workingDay =
            findWorkingDay(availability.days, date)
                ?: return emptyList()

        if (!workingDay.isOpen) {
            return emptyList()
        }

        val opening =
            parseTime(workingDay.startTime)
                ?: return emptyList()
        val closing =
            parseTime(workingDay.endTime)
                ?: return emptyList()
        val now =
            LocalTime.now(clock)
        val today =
            LocalDate.now(clock)
        val result = mutableListOf<GeneratedSlot>()
        var candidate = opening

        while (
            !candidate
                .plusMinutes(durationMinutes.toLong())
                .isAfter(closing)
        ) {
            val segments =
                segmentsFor(
                    start = candidate,
                    durationMinutes = durationMinutes,
                    intervalMinutes = intervalMinutes
                )
            val inFuture =
                date.isAfter(today) ||
                    (
                        date == today &&
                            candidate.isAfter(now)
                        )
            val hasConflict =
                segments.any(occupiedTimes::contains)

            result += GeneratedSlot(
                time = candidate.format(timeFormatter),
                occupiedSegments = segments,
                isAvailable = inFuture && !hasConflict
            )
            candidate =
                candidate.plusMinutes(intervalMinutes.toLong())
        }

        return result
    }

    fun availableTimes(
        availability: BarberAvailability,
        date: LocalDate,
        durationMinutes: Int,
        occupiedTimes: Set<String>,
        clock: Clock = Clock.systemDefaultZone()
    ): List<String> {
        return generate(
            availability = availability,
            date = date,
            durationMinutes = durationMinutes,
            occupiedTimes = occupiedTimes,
            clock = clock
        )
            .filter(GeneratedSlot::isAvailable)
            .map(GeneratedSlot::time)
    }

    private fun findWorkingDay(
        days: List<DayAvailability>,
        date: LocalDate
    ): DayAvailability? {
        val dayName =
            date.dayOfWeek.getDisplayName(
                TextStyle.FULL,
                Locale.ENGLISH
            )

        return days.find { day ->
            day.day.equals(dayName, ignoreCase = true)
        }
    }

    private fun segmentsFor(
        start: LocalTime,
        durationMinutes: Int,
        intervalMinutes: Int
    ): List<String> {
        val count =
            (durationMinutes + intervalMinutes - 1) /
                intervalMinutes

        return (0 until count).map { index ->
            start
                .plusMinutes(
                    index.toLong() * intervalMinutes
                )
                .format(timeFormatter)
        }
    }

    private fun parseTime(value: String): LocalTime? {
        return try {
            LocalTime.parse(value, timeFormatter)
        } catch (_: DateTimeParseException) {
            null
        }
    }
}
