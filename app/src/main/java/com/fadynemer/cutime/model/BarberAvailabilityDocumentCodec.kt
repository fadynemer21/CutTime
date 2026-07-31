package com.fadynemer.cutime.model

/**
 * Firestore-neutral encoding for weekly availability documents.
 *
 * Keeping this conversion free of Firebase types makes the migration from the
 * legacy single-period schema independently unit testable.
 */
object BarberAvailabilityDocumentCodec {
    const val CURRENT_SCHEMA_VERSION = 2

    fun encodeDay(day: DayAvailability): Map<String, Any> {
        val periods = day.effectiveWorkingPeriods()
        val first = periods.first()
        return mapOf(
            "day" to day.day,
            "isOpen" to day.isOpen,
            // Retained for safe reads by app versions released before schema 2.
            "startTime" to first.startTime,
            "endTime" to first.endTime,
            "workingPeriods" to periods.map { period ->
                mapOf(
                    "startTime" to period.startTime,
                    "endTime" to period.endTime
                )
            }
        )
    }

    fun decodeDay(raw: Any?): DayAvailability? {
        val map = raw as? Map<*, *> ?: return null
        val day = map["day"] as? String ?: return null
        val isOpen = map["isOpen"] as? Boolean ?: false
        val legacyStart = map["startTime"] as? String ?: "09:00"
        val legacyEnd = map["endTime"] as? String ?: "17:00"
        val periods =
            (map["workingPeriods"] as? List<*>)
                ?.mapNotNull(::decodePeriod)
                .orEmpty()

        return DayAvailability(
            day = day,
            isOpen = isOpen,
            startTime = periods.firstOrNull()?.startTime ?: legacyStart,
            endTime = periods.firstOrNull()?.endTime ?: legacyEnd,
            workingPeriods = periods
        )
    }

    fun decode(
        rawDays: Any?,
        rawBlockedDates: Any?
    ): BarberAvailability {
        val days =
            (rawDays as? List<*>)
                ?.mapNotNull(::decodeDay)
                ?.takeIf(List<DayAvailability>::isNotEmpty)
                ?: defaultWorkingWeek()
        val blockedDates =
            (rawBlockedDates as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()
        return BarberAvailability(
            days = days,
            blockedDates = blockedDates
        )
    }

    private fun decodePeriod(raw: Any?): WorkingPeriod? {
        val map = raw as? Map<*, *> ?: return null
        val start = map["startTime"] as? String ?: return null
        val end = map["endTime"] as? String ?: return null
        return WorkingPeriod(startTime = start, endTime = end)
    }
}
