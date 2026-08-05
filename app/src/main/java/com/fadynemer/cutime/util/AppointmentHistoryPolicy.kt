package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.AppointmentStatus

object AppointmentHistoryPolicy {

    fun canCustomerHide(
        authenticatedUserId: String,
        appointmentCustomerId: String?,
        appointmentStatus: String?,
        alreadyHidden: Boolean,
        appointmentEndAtMillis: Long? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val isPastUpcoming =
            appointmentStatus == AppointmentStatus.UPCOMING.name &&
                appointmentEndAtMillis != null &&
                appointmentEndAtMillis <= nowMillis
        return authenticatedUserId == appointmentCustomerId &&
            (
                appointmentStatus in setOf(
                    AppointmentStatus.COMPLETED.name,
                    AppointmentStatus.CANCELLED.name
                ) || isPastUpcoming
                ) &&
            !alreadyHidden
    }

    fun canBarberHide(
        authenticatedUserId: String,
        appointmentBarberId: String?,
        appointmentStatus: String?,
        alreadyHidden: Boolean
    ): Boolean {
        return authenticatedUserId == appointmentBarberId &&
            appointmentStatus in setOf(
                AppointmentStatus.COMPLETED.name,
                AppointmentStatus.CANCELLED.name
            ) &&
            !alreadyHidden
    }
}
