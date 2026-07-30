package com.fadynemer.cutime.model

data class Rating(
    val id: String,
    val appointmentId: String,
    val customerId: String,
    val barberId: String,
    val customerName: String,
    val stars: Int,
    val review: String,
    val createdAtMillis: Long
)

data class RatingSummary(
    val average: Double = 0.0,
    val count: Int = 0
)

data class RatingRequest(
    val appointmentId: String,
    val barberId: String,
    val stars: Int,
    val review: String
)
