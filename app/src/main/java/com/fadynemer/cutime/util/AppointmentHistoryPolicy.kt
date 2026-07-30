package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.AppointmentStatus

object AppointmentHistoryPolicy {

    fun canCustomerHide(
        authenticatedUserId: String,
        appointmentCustomerId: String?,
        appointmentStatus: String?,
        alreadyHidden: Boolean
    ): Boolean {
        return authenticatedUserId == appointmentCustomerId &&
            appointmentStatus == AppointmentStatus.CANCELLED.name &&
            !alreadyHidden
    }
}
