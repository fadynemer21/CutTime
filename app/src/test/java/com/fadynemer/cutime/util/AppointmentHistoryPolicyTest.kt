package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.AppointmentStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentHistoryPolicyTest {

    @Test
    fun customerCanHideOwnCancelledAppointment() {
        assertTrue(
            AppointmentHistoryPolicy.canCustomerHide(
                authenticatedUserId = "customer_1",
                appointmentCustomerId = "customer_1",
                appointmentStatus =
                    AppointmentStatus.CANCELLED.name,
                alreadyHidden = false
            )
        )
    }

    @Test
    fun upcomingOrCompletedAppointmentCannotBeHidden() {
        assertFalse(
            AppointmentHistoryPolicy.canCustomerHide(
                authenticatedUserId = "customer_1",
                appointmentCustomerId = "customer_1",
                appointmentStatus =
                    AppointmentStatus.UPCOMING.name,
                alreadyHidden = false
            )
        )
        assertFalse(
            AppointmentHistoryPolicy.canCustomerHide(
                authenticatedUserId = "customer_1",
                appointmentCustomerId = "customer_1",
                appointmentStatus =
                    AppointmentStatus.COMPLETED.name,
                alreadyHidden = false
            )
        )
    }

    @Test
    fun anotherCustomerOrAlreadyHiddenRecordCannotBeHidden() {
        assertFalse(
            AppointmentHistoryPolicy.canCustomerHide(
                authenticatedUserId = "customer_2",
                appointmentCustomerId = "customer_1",
                appointmentStatus =
                    AppointmentStatus.CANCELLED.name,
                alreadyHidden = false
            )
        )
        assertFalse(
            AppointmentHistoryPolicy.canCustomerHide(
                authenticatedUserId = "customer_1",
                appointmentCustomerId = "customer_1",
                appointmentStatus =
                    AppointmentStatus.CANCELLED.name,
                alreadyHidden = true
            )
        )
    }
}
