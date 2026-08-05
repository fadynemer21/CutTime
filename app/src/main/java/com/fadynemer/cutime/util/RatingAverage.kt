package com.fadynemer.cutime.util

import kotlin.math.floor

object RatingAverage {
    fun roundToHalf(value: Double): Double {
        if (!value.isFinite() || value <= 0.0) return 0.0
        return floor(value * 2.0 + 0.5) / 2.0
    }

    fun format(value: Double): String {
        val rounded = roundToHalf(value)
        return if (rounded % 1.0 == 0.0) {
            rounded.toInt().toString()
        } else {
            rounded.toString()
        }
    }
}
