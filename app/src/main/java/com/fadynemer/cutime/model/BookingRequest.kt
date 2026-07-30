package com.fadynemer.cutime.model

data class BookingRequest(
    val barberId: String,
    val barberName: String,
    val serviceId: String,
    val serviceName: String,
    val price: Int,
    val durationMinutes: Int,
    val appointmentDate: String,
    val appointmentTime: String
)

data class RescheduleRequest(
    val appointmentId: String,
    val appointmentDate: String,
    val appointmentTime: String
)
