package com.fadynemer.cutime.model

data class Appointment(
    val id: String,
    val customerId: String,
    val customerName: String = "Customer",
    val customerEmail: String = "",
    val barberId: String,
    val barberName: String,
    val serviceId: String,
    val serviceName: String,
    val price: Int,
    val durationMinutes: Int,
    val appointmentDate: String,
    val appointmentTime: String,
    val startAtMillis: Long,
    val endAtMillis: Long,
    val status: AppointmentStatus,
    val hiddenFromCustomer: Boolean = false,
    val ratingId: String? = null,
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
    val rescheduledAtMillis: Long? = null
)

enum class AppointmentStatus {
    UPCOMING,
    COMPLETED,
    CANCELLED;

    companion object {
        fun fromFirestore(value: String?): AppointmentStatus {
            return entries.firstOrNull { status ->
                status.name == value
            } ?: UPCOMING
        }
    }
}

data class AppointmentGroups(
    val upcoming: List<Appointment> = emptyList(),
    val completed: List<Appointment> = emptyList(),
    val cancelled: List<Appointment> = emptyList()
)
