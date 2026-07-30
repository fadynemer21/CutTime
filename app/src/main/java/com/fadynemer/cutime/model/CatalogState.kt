package com.fadynemer.cutime.model

enum class CatalogSource {
    FIRESTORE,
    DEVELOPMENT_FALLBACK
}

data class BarberCatalog(
    val barbers: List<BarberShop> = emptyList(),
    val source: CatalogSource = CatalogSource.FIRESTORE
)

data class OccupiedSlot(
    val barberId: String,
    val appointmentDate: String,
    val appointmentTime: String
)
