package com.fadynemer.cutime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberCatalogCacheTest {
    @Test
    fun productionLookup_doesNotReturnDevelopmentPreview() {
        BarberCatalogCache.replace(emptyList())

        val barber = BarberCatalogCache.find(
            barberId = "barber_1",
            includeDevelopmentFallback = false
        )

        assertEquals(null, barber)
    }

    @Test
    fun missingCachedBarber_usesMarkedDevelopmentPreview() {
        BarberCatalogCache.replace(emptyList())

        val barber = BarberCatalogCache.find("barber_1")

        assertNotNull(barber)
        assertTrue(barber!!.isDevelopmentFallback)
    }

    @Test
    fun cachedRealBarber_isReturnedWithoutPreviewFlag() {
        val real = SampleBarberData.barberShops.first()
            .copy(
                id = "real_barber",
                name = "Firestore Barber",
                isDevelopmentFallback = false
            )
        BarberCatalogCache.replace(listOf(real))

        val barber = BarberCatalogCache.find("real_barber")

        assertEquals(real, barber)
        assertFalse(barber!!.isDevelopmentFallback)
    }

    @Test
    fun replacingCatalog_removesOldRealEntries() {
        val old = SampleBarberData.barberShops.first()
            .copy(id = "old_real")
        val current = SampleBarberData.barberShops.first()
            .copy(id = "current_real")
        BarberCatalogCache.replace(listOf(old))

        BarberCatalogCache.replace(listOf(current))

        assertEquals(
            listOf("current_real"),
            BarberCatalogCache.all().map { it.id }
        )
        assertEquals(null, BarberCatalogCache.find("old_real"))
    }

    @Test
    fun developmentPreview_keepsOriginalSampleContent() {
        BarberCatalogCache.replace(emptyList())
        val original = SampleBarberData.findById("barber_2")!!

        val preview = BarberCatalogCache.find("barber_2")!!

        assertEquals(original.name, preview.name)
        assertEquals(original.services, preview.services)
        assertEquals(original.openingHours, preview.openingHours)
        assertTrue(preview.isDevelopmentFallback)
    }
}
