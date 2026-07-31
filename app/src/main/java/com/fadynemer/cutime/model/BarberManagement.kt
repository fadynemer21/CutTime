package com.fadynemer.cutime.model

data class ManagedBarberProfile(
    val uid: String = "",
    val shopName: String = "",
    val description: String = ""
)

/**
 * A continuous period during which the barber accepts appointments.
 *
 * Breaks are represented by gaps between periods. For example, periods
 * 09:00-12:00 and 14:00-19:00 create a two-hour break without needing a
 * second, potentially conflicting break model.
 */
data class WorkingPeriod(
    val startTime: String,
    val endTime: String
)

data class DayAvailability(
    val day: String,
    val isOpen: Boolean,
    val startTime: String,
    val endTime: String,
    val workingPeriods: List<WorkingPeriod> = emptyList()
)

/**
 * Returns the new multi-period schedule when present and transparently
 * upgrades legacy documents that only contain startTime/endTime.
 */
fun DayAvailability.effectiveWorkingPeriods(): List<WorkingPeriod> {
    return workingPeriods.ifEmpty {
        listOf(
            WorkingPeriod(
                startTime = startTime,
                endTime = endTime
            )
        )
    }
}

/** Keeps legacy fields aligned with the first period for older app builds. */
fun DayAvailability.withWorkingPeriods(
    periods: List<WorkingPeriod>
): DayAvailability {
    val first = periods.firstOrNull()
    return copy(
        startTime = first?.startTime ?: startTime,
        endTime = first?.endTime ?: endTime,
        workingPeriods = periods
    )
}

data class BarberAvailability(
    val days: List<DayAvailability> = defaultWorkingWeek(),
    val blockedDates: List<String> = emptyList()
)

data class AvailabilitySaveResult(
    val cancelledAppointmentCount: Int = 0
)

fun defaultWorkingWeek(): List<DayAvailability> {
    return listOf(
        DayAvailability("Sunday", true, "09:00", "19:00"),
        DayAvailability("Monday", true, "09:00", "19:00"),
        DayAvailability("Tuesday", true, "09:00", "19:00"),
        DayAvailability("Wednesday", true, "09:00", "19:00"),
        DayAvailability("Thursday", true, "09:00", "19:00"),
        DayAvailability("Friday", true, "09:00", "14:00"),
        DayAvailability("Saturday", false, "09:00", "19:00")
    )
}
