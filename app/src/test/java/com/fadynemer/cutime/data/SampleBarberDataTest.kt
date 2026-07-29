package com.fadynemer.cutime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleBarberDataTest {

    @Test
    fun barberIds_areUnique() {
        val barberIds = SampleBarberData.barberShops.map { barberShop ->
            barberShop.id
        }

        assertEquals(
            barberIds.size,
            barberIds.distinct().size
        )
    }

    @Test
    fun findById_returnsMatchingBarber() {
        val expectedBarber = SampleBarberData.barberShops.first()

        val result = SampleBarberData.findById(expectedBarber.id)

        assertEquals(expectedBarber, result)
    }

    @Test
    fun findById_returnsNullForUnknownId() {
        assertNull(
            SampleBarberData.findById("unknown_barber")
        )
    }

    @Test
    fun everyBarber_hasBookableProfileInformation() {
        assertTrue(
            SampleBarberData.barberShops.all { barberShop ->
                barberShop.description.isNotBlank() &&
                    barberShop.services.isNotEmpty() &&
                    barberShop.openingHours.isNotEmpty() &&
                    barberShop.availableTimes.isNotEmpty()
            }
        )
    }
}
