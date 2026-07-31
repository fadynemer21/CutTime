package com.fadynemer.cutime.model

data class BarberShopReadiness(
    val profileComplete: Boolean = false,
    val validServiceCount: Int = 0,
    val availabilitySaved: Boolean = false,
    val hasOpenWorkingDay: Boolean = false
) {
    val servicesComplete: Boolean
        get() = validServiceCount > 0

    val availabilityComplete: Boolean
        get() = availabilitySaved && hasOpenWorkingDay

    val isBookable: Boolean
        get() =
            profileComplete &&
                servicesComplete &&
                availabilityComplete

    val completedStepCount: Int
        get() =
            listOf(
                profileComplete,
                servicesComplete,
                availabilityComplete
            ).count { it }

    val totalStepCount: Int
        get() = 3
}

object BarberShopReadinessEvaluator {
    fun profileComplete(
        shopName: String?,
        description: String?
    ): Boolean {
        val cleanName = shopName.orEmpty().trim()
        val cleanDescription = description.orEmpty().trim()
        return (
            cleanName.length >= 2 &&
                cleanDescription.length in 10..500
            )
    }

    fun validServiceCount(
        services: List<Map<String, Any?>>
    ): Int {
        return services.count(::isValidService)
    }

    fun availabilityState(
        exists: Boolean,
        rawDays: List<Map<String, Any?>>
    ): Pair<Boolean, Boolean> {
        if (!exists || rawDays.size != 7) {
            return false to false
        }

        val allDaysValid = rawDays.all(::isValidDay)
        val hasOpenDay = rawDays.any { day ->
            day["isOpen"] == true &&
                validWorkingPeriods(day)
        }
        return allDaysValid to (allDaysValid && hasOpenDay)
    }

    private fun isValidService(
        service: Map<String, Any?>
    ): Boolean {
        val name = service["name"] as? String
        val price = (service["price"] as? Number)?.toInt()
        val duration =
            (service["durationMinutes"] as? Number)?.toInt()
        return (
            !name.isNullOrBlank() &&
                name.trim().length >= 2 &&
                price != null &&
                price > 0 &&
                duration != null &&
                duration > 0 &&
                duration % 15 == 0
            )
    }

    private fun isValidDay(
        day: Map<String, Any?>
    ): Boolean {
        val name = day["day"] as? String
        val isOpen = day["isOpen"] as? Boolean
        val start = day["startTime"] as? String
        val end = day["endTime"] as? String

        if (
            name.isNullOrBlank() ||
            isOpen == null ||
            start == null ||
            end == null
        ) {
            return false
        }

        return !isOpen || validWorkingPeriods(day)
    }

    private fun validWorkingPeriods(
        day: Map<String, Any?>
    ): Boolean {
        val rawPeriods = day["workingPeriods"] as? List<*>
        if (rawPeriods.isNullOrEmpty()) {
            return validOpenInterval(
                day["startTime"] as? String,
                day["endTime"] as? String
            )
        }
        if (rawPeriods.size > 6) return false

        var previousEnd: Int? = null
        rawPeriods.forEach { raw ->
            val period = raw as? Map<*, *> ?: return false
            val start = parseTime(period["startTime"] as? String)
                ?: return false
            val end = parseTime(period["endTime"] as? String)
                ?: return false
            if (start >= end) return false
            if (previousEnd != null && start < previousEnd) {
                return false
            }
            previousEnd = end
        }
        return true
    }

    private fun validOpenInterval(
        start: String?,
        end: String?
    ): Boolean {
        val startMinutes = parseTime(start) ?: return false
        val endMinutes = parseTime(end) ?: return false
        return startMinutes < endMinutes
    }

    private fun parseTime(value: String?): Int? {
        if (value == null) return null
        val parts = value.split(':')
        if (parts.size != 2) return null
        val hour = parts[0].toIntOrNull() ?: return null
        val minute = parts[1].toIntOrNull() ?: return null
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }
}
