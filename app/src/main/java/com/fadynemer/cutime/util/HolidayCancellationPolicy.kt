package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.AppointmentStatus

object HolidayCancellationPolicy {

    fun shouldCancel(
        status: String?,
        appointmentDate: String?,
        blockedDates: Set<String>
    ): Boolean {
        return status == AppointmentStatus.UPCOMING.name &&
            appointmentDate != null &&
            appointmentDate in blockedDates
    }

    fun requiredBatchWrites(
        affectedSlotCounts: Collection<Int>,
        baseWrites: Int = 2
    ): Int {
        return baseWrites +
            affectedSlotCounts.size +
            affectedSlotCounts.sum()
    }
}
