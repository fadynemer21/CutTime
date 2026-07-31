package com.fadynemer.cutime.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberAvailabilityDocumentCodecTest {

    @Test
    fun legacyDay_decodesAsOneEffectiveWorkingPeriod() {
        val decoded = BarberAvailabilityDocumentCodec.decodeDay(
            mapOf(
                "day" to "Wednesday",
                "isOpen" to true,
                "startTime" to "09:00",
                "endTime" to "19:00"
            )
        )!!

        assertTrue(decoded.workingPeriods.isEmpty())
        assertEquals(
            listOf(WorkingPeriod("09:00", "19:00")),
            decoded.effectiveWorkingPeriods()
        )
    }

    @Test
    fun versionTwoDay_roundTripsAllPeriods() {
        val original = DayAvailability(
            day = "Wednesday",
            isOpen = true,
            startTime = "09:00",
            endTime = "12:00",
            workingPeriods = listOf(
                WorkingPeriod("09:00", "12:00"),
                WorkingPeriod("14:00", "16:00"),
                WorkingPeriod("16:30", "19:00")
            )
        )

        val decoded = BarberAvailabilityDocumentCodec.decodeDay(
            BarberAvailabilityDocumentCodec.encodeDay(original)
        )

        assertEquals(original, decoded)
    }

    @Test
    fun encodedLegacyFields_useFirstPeriodToAvoidExposingBreaks() {
        val encoded = BarberAvailabilityDocumentCodec.encodeDay(
            DayAvailability(
                day = "Wednesday",
                isOpen = true,
                startTime = "09:00",
                endTime = "19:00",
                workingPeriods = listOf(
                    WorkingPeriod("09:00", "12:00"),
                    WorkingPeriod("14:00", "19:00")
                )
            )
        )

        assertEquals("09:00", encoded["startTime"])
        assertEquals("12:00", encoded["endTime"])
        assertEquals(2, (encoded["workingPeriods"] as List<*>).size)
    }
}
