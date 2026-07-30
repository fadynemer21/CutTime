package com.fadynemer.cutime.data

import com.fadynemer.cutime.model.BarberShop

object BarberCatalogCache {
    private val cachedBarbers =
        linkedMapOf<String, BarberShop>()

    @Synchronized
    fun replace(barbers: List<BarberShop>) {
        cachedBarbers.clear()
        barbers.forEach { barber ->
            cachedBarbers[barber.id] = barber
        }
    }

    @Synchronized
    fun find(barberId: String): BarberShop? {
        return cachedBarbers[barberId]
            ?: SampleBarberData.findById(barberId)
                ?.copy(isDevelopmentFallback = true)
    }

    @Synchronized
    fun all(): List<BarberShop> {
        return cachedBarbers.values.toList()
    }
}
