package com.fadynemer.cutime.util

import org.junit.Assert.assertEquals
import org.junit.Test

class RatingAverageTest {
    @Test
    fun threePointThreeRoundsUpToThreePointFive() {
        assertEquals(
            3.5,
            RatingAverage.roundToHalf(3.333333),
            0.0
        )
    }

    @Test
    fun threePointTwoRoundsDownToThree() {
        assertEquals(
            3.0,
            RatingAverage.roundToHalf(3.222222),
            0.0
        )
    }

    @Test
    fun formatUsesOnlyWholeOrHalfValues() {
        assertEquals("3", RatingAverage.format(3.222222))
        assertEquals("3.5", RatingAverage.format(3.333333))
        assertEquals("0", RatingAverage.format(0.0))
    }
}
