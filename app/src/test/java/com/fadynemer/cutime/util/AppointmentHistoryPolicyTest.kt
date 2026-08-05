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
    fun activeUpcomingCannotBeHiddenButExpiredUpcomingAndCompletedCan() {
        assertFalse(
            AppointmentHistoryPolicy.canCustomerHide(
                authenticatedUserId = "customer_1",
                appointmentCustomerId = "customer_1",
                appointmentStatus =
                    AppointmentStatus.UPCOMING.name,
                alreadyHidden = false,
                appointmentEndAtMillis = 2_000L,
                nowMillis = 1_000L
            )
        )
        assertTrue(
            AppointmentHistoryPolicy.canCustomerHide(
                authenticatedUserId = "customer_1",
                appointmentCustomerId = "customer_1",
                appointmentStatus =
                    AppointmentStatus.UPCOMING.name,
                alreadyHidden = false,
                appointmentEndAtMillis = 1_000L,
                nowMillis = 2_000L
            )
        )
        assertTrue(
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
    fun barberCanHideOwnCompletedOrCancelledHistory() {
        assertTrue(
            AppointmentHistoryPolicy.canBarberHide(
                authenticatedUserId = "barber_1",
                appointmentBarberId = "barber_1",
                appointmentStatus = AppointmentStatus.COMPLETED.name,
                alreadyHidden = false
            )
        )
        assertTrue(
            AppointmentHistoryPolicy.canBarberHide(
                authenticatedUserId = "barber_1",
                appointmentBarberId = "barber_1",
                appointmentStatus = AppointmentStatus.CANCELLED.name,
                alreadyHidden = false
            )
        )
        assertFalse(
            AppointmentHistoryPolicy.canBarberHide(
                authenticatedUserId = "barber_2",
                appointmentBarberId = "barber_1",
                appointmentStatus = AppointmentStatus.COMPLETED.name,
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
