package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.ManagedBarberProfile
import com.fadynemer.cutime.model.effectiveWorkingPeriods
import java.time.LocalDate
import java.time.LocalTime
import java.time.Duration
import java.time.format.DateTimeParseException

object BarberManagementValidator {

    fun validateProfile(
        profile: ManagedBarberProfile
    ): String? {
        return when {
            profile.shopName.trim().length < 2 ->
                "Enter a shop name with at least 2 characters."

            profile.description.trim().length < 10 ->
                "Enter a description with at least 10 characters."

            profile.description.length > 500 ->
                "Keep the description under 500 characters."

            else -> null
        }
    }

    fun validateService(
        service: BarberService
    ): String? {
        return when {
            service.name.trim().length < 2 ->
                "Enter a service name."

            service.price <= 0 ->
                "Price must be greater than zero."

            service.durationMinutes !in
                listOf(15, 30, 45, 60, 75, 90, 105, 120) ->
                "Duration must use 15-minute intervals."

            else -> null
        }
    }

    fun validateAvailability(
        availability: BarberAvailability
    ): String? {
        val duplicateDays =
            availability.days
                .groupingBy { day -> day.day }
                .eachCount()
                .any { entry -> entry.value > 1 }

        if (duplicateDays) {
            return "Each weekday can appear only once."
        }

        availability.days
            .filter { day -> day.isOpen }
            .forEach { day ->
                val periods = day.effectiveWorkingPeriods()
                if (periods.isEmpty()) {
                    return "Add at least one work period for ${day.day}."
                }
                if (periods.size > MAX_WORKING_PERIODS_PER_DAY) {
                    return "${day.day} can have at most $MAX_WORKING_PERIODS_PER_DAY work periods."
                }

                var previousEnd: LocalTime? = null
                periods.forEachIndexed { index, period ->
                    val periodNumber = index + 1
                    val start =
                        parseTime(period.startTime)
                            ?: return "Invalid start time for ${day.day} period $periodNumber."
                    val end =
                        parseTime(period.endTime)
                            ?: return "Invalid end time for ${day.day} period $periodNumber."

                    if (!start.isBefore(end)) {
                        return if (periods.size == 1) {
                            "${day.day} closing time must be after opening time."
                        } else {
                            "${day.day} period $periodNumber must end after it starts."
                        }
                    }
                    if (previousEnd != null && start.isBefore(previousEnd)) {
                        return "${day.day} work periods cannot overlap and must be in time order."
                    }
                    previousEnd = end
                }
            }

        val invalidBlockedDate =
            availability.blockedDates.firstOrNull { value ->
                try {
                    LocalDate.parse(value)
                    false
                } catch (_: DateTimeParseException) {
                    true
                }
            }

        return if (invalidBlockedDate != null) {
            "Blocked dates must use YYYY-MM-DD."
        } else {
            null
        }
    }

    fun isBookable(
        availability: BarberAvailability,
        date: LocalDate,
        time: LocalTime,
        durationMinutes: Int
    ): Boolean {
        if (durationMinutes <= 0) return false
        if (date.toString() in availability.blockedDates) {
            return false
        }

        val englishDay =
            date.dayOfWeek.name
                .lowercase()
                .replaceFirstChar(Char::uppercase)
        val workingDay =
            availability.days.find { day ->
                day.day == englishDay
            } ?: return false

        if (!workingDay.isOpen) {
            return false
        }

        return workingDay.effectiveWorkingPeriods().any { period ->
            val start = parseTime(period.startTime)
                ?: return@any false
            val end = parseTime(period.endTime)
                ?: return@any false
            !time.isBefore(start) &&
                Duration.between(time, end).toMinutes() >= durationMinutes
        }
    }

    private fun parseTime(
        value: String
    ): LocalTime? {
        return try {
            LocalTime.parse(value)
        } catch (_: DateTimeParseException) {
            null
        }
    }

    const val MAX_WORKING_PERIODS_PER_DAY = 6
}
