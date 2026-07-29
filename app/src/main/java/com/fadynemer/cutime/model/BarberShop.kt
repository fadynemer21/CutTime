package com.fadynemer.cutime.model

data class BarberShop(
    val id: String,
    val name: String,
    val rating: Double,
    val reviewCount: Int,
    val startingPrice: Int,
    val nextAvailable: String
)