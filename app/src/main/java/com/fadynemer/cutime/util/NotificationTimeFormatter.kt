package com.fadynemer.cutime.util

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object NotificationTimeFormatter {
    private val timeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH)
    private val dateFormatter =
        DateTimeFormatter.ofPattern(
            "d MMM yyyy, HH:mm",
            Locale.ENGLISH
        )

    fun format(
        timestampMillis: Long,
        clock: Clock = Clock.systemDefaultZone()
    ): String {
        if (timestampMillis <= 0L) {
            return "Just now"
        }

        val zone = clock.zone
        val dateTime =
            Instant.ofEpochMilli(timestampMillis)
                .atZone(zone)
        val today = LocalDate.now(clock)

        return when (dateTime.toLocalDate()) {
            today ->
                "Today at ${dateTime.format(timeFormatter)}"

            today.minusDays(1) ->
                "Yesterday at ${dateTime.format(timeFormatter)}"

            else -> dateTime.format(dateFormatter)
        }
    }

    fun dayGroup(
        timestampMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
        today: LocalDate = LocalDate.now(zoneId)
    ): String {
        if (timestampMillis <= 0L) return "Recent"
        val date =
            Instant.ofEpochMilli(timestampMillis)
                .atZone(zoneId)
                .toLocalDate()

        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            date >= today.minusDays(7) -> "This week"
            else -> "Earlier"
        }
    }
}
