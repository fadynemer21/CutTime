package com.fadynemer.cutime.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppRoutePolicyTest {
    @Test
    fun authenticatedTopLevelDestinationsAreAllowed() {
        listOf(
            AppRoute.CustomerHome.pattern,
            AppRoute.CustomerAppointments.pattern,
            AppRoute.CustomerProfile.pattern,
            AppRoute.BarberDashboard.pattern,
            AppRoute.BarberServices.pattern,
            AppRoute.BarberAvailability.pattern,
            AppRoute.BarberManageProfile.pattern,
            AppRoute.BarberGallery.pattern
        ).forEach { route ->
            assertTrue(route, AppRoutePolicy.isAllowed(route))
        }
    }

    @Test
    fun generatedArgumentDestinationsAreAllowed() {
        listOf(
            AppRoute.BarberProfile.create("barber one"),
            AppRoute.Booking.create("barber/two"),
            AppRoute.CustomerAppointmentDetail.create("appointment 1"),
            AppRoute.BarberAppointmentDetail.create("appointment 1"),
            AppRoute.Reschedule.create("appointment 1"),
            AppRoute.Rating.create("appointment 1"),
            AppRoute.Notifications.create(false),
            AppRoute.NotificationSettings.create(true)
        ).forEach { route ->
            assertTrue(route, AppRoutePolicy.isAllowed(route))
        }
    }

    @Test
    fun authenticationRoutesCannotBeOpenedFromRemotePayloads() {
        listOf(
            AppRoute.Splash.pattern,
            AppRoute.Welcome.pattern,
            AppRoute.Login.pattern,
            AppRoute.Register.pattern
        ).forEach { route ->
            assertFalse(route, AppRoutePolicy.isAllowed(route))
        }
    }

    @Test
    fun malformedAndUnknownRoutesAreRejected() {
        listOf(
            null,
            "",
            " ",
            "/appointment/one",
            "appointment/",
            "appointment/one/two",
            "appointment/../profile",
            "appointment\\one",
            "appointment/one?admin=true",
            "appointment/one#fragment",
            "unknown",
            "https://example.com"
        ).forEach { route ->
            assertFalse(route.orEmpty(), AppRoutePolicy.isAllowed(route))
        }
    }

    @Test
    fun oversizedRemoteRouteIsRejected() {
        assertFalse(
            AppRoutePolicy.isAllowed("appointment/${"a".repeat(600)}")
        )
    }
}
