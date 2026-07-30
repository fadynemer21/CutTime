package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.ManagedBarberProfile
import java.time.LocalDate
import java.time.LocalTime
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
                val start =
                    parseTime(day.startTime)
                        ?: return "Invalid start time for ${day.day}."
                val end =
                    parseTime(day.endTime)
                        ?: return "Invalid end time for ${day.day}."

                if (!start.isBefore(end)) {
                    return "${day.day} closing time must be after opening time."
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

        val start = parseTime(workingDay.startTime)
            ?: return false
        val end = parseTime(workingDay.endTime)
            ?: return false
        val appointmentEnd =
            time.plusMinutes(durationMinutes.toLong())

        return !time.isBefore(start) &&
            !appointmentEnd.isAfter(end)
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
}
