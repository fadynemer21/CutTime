package com.fadynemer.cutime.model

data class ManagedBarberProfile(
    val uid: String = "",
    val shopName: String = "",
    val description: String = ""
)

data class DayAvailability(
    val day: String,
    val isOpen: Boolean,
    val startTime: String,
    val endTime: String
)

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
