package com.fadynemer.cutime.model

data class BarberShop(
    val id: String,
    val name: String,
    val rating: Double,
    val reviewCount: Int,
    val startingPrice: Int,
    val nextAvailable: String,
    val description: String,
    val services: List<BarberService>,
    val openingHours: List<OpeningHours>,
    val galleryItemCount: Int,
    val availableTimes: List<String>
)

data class BarberService(
    val id: String,
    val name: String,
    val price: Int,
    val durationMinutes: Int
)

data class OpeningHours(
    val day: String,
    val hours: String
)
