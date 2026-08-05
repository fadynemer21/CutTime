package com.fadynemer.cutime.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private fun encodeRouteArgument(value: String): String {
    return URLEncoder.encode(
        value,
        StandardCharsets.UTF_8.name()
    ).replace("+", "%20")
}

sealed class AppRoute(
    val pattern: String
) {
    data object Splash : AppRoute("splash")
    data object Welcome : AppRoute("welcome")
    data object Login : AppRoute("login")
    data object Register : AppRoute("register")
    data object CustomerHome : AppRoute("home")
    data object CustomerAppointments : AppRoute("appointments")
    data object CustomerProfile : AppRoute("customer_profile")
    data object BarberDashboard : AppRoute("dashboard")
    data object BarberServices : AppRoute("barber_services")
    data object BarberAvailability : AppRoute("barber_availability")
    data object BarberManageProfile :
        AppRoute("barber_manage_profile")
    data object BarberAppointmentHistory :
        AppRoute("barber_appointment_history")
    data object BarberGallery : AppRoute("barber_gallery")
    data object Notifications :
        AppRoute("notifications/{mode}") {
        fun create(isBarberMode: Boolean) =
            "notifications/${if (isBarberMode) "barber" else "customer"}"
    }

    data object NotificationSettings :
        AppRoute("notification_settings/{mode}") {
        fun create(isBarberMode: Boolean) =
            "notification_settings/${if (isBarberMode) "barber" else "customer"}"
    }

    data object BarberProfile :
        AppRoute("barber_profile/{barberId}") {
        fun create(barberId: String) =
            "barber_profile/${encodeRouteArgument(barberId)}"
    }

    data object BarberReviews :
        AppRoute("barber_reviews/{barberId}") {
        fun create(barberId: String) =
            "barber_reviews/${encodeRouteArgument(barberId)}"
    }

    data object Booking :
        AppRoute("booking/{barberId}") {
        fun create(barberId: String) =
            "booking/${encodeRouteArgument(barberId)}"
    }

    data object CustomerAppointmentDetail :
        AppRoute("appointment/{appointmentId}") {
        fun create(appointmentId: String) =
            "appointment/${encodeRouteArgument(appointmentId)}"
    }

    data object BarberAppointmentDetail :
        AppRoute("barber_appointment/{appointmentId}") {
        fun create(appointmentId: String) =
            "barber_appointment/${encodeRouteArgument(appointmentId)}"
    }

    data object Reschedule :
        AppRoute("reschedule/{appointmentId}") {
        fun create(appointmentId: String) =
            "reschedule/${encodeRouteArgument(appointmentId)}"
    }

    data object Rating :
        AppRoute("rating/{appointmentId}") {
        fun create(appointmentId: String) =
            "rating/${encodeRouteArgument(appointmentId)}"
    }
}

object RouteArguments {
    const val BARBER_ID = "barberId"
    const val APPOINTMENT_ID = "appointmentId"
    const val MODE = "mode"
}

/**
 * Restricts notification/deep-link input to authenticated in-app destinations.
 * FCM payloads are remote input and must never be passed unchecked to NavHost.
 */
object AppRoutePolicy {
    private val exactDestinations = setOf(
        AppRoute.CustomerHome.pattern,
        AppRoute.CustomerAppointments.pattern,
        AppRoute.CustomerProfile.pattern,
        AppRoute.BarberDashboard.pattern,
        AppRoute.BarberServices.pattern,
        AppRoute.BarberAvailability.pattern,
        AppRoute.BarberManageProfile.pattern,
        AppRoute.BarberAppointmentHistory.pattern,
        AppRoute.BarberGallery.pattern
    )

    private val argumentPrefixes = setOf(
        "barber_profile/",
        "barber_reviews/",
        "booking/",
        "appointment/",
        "barber_appointment/",
        "reschedule/",
        "rating/",
        "notifications/",
        "notification_settings/"
    )

    fun isAllowed(destination: String?): Boolean {
        val route = destination?.trim() ?: return false
        if (
            route.isEmpty() ||
            route.length > MAX_ROUTE_LENGTH ||
            route.startsWith("/") ||
            route.contains('?') ||
            route.contains('#') ||
            route.contains('\\') ||
            route.split('/').any { it == "." || it == ".." }
        ) {
            return false
        }

        if (route in exactDestinations) return true

        return argumentPrefixes.any { prefix ->
            route.startsWith(prefix) &&
                route.length > prefix.length &&
                !route.substring(prefix.length).contains('/')
        }
    }

    private const val MAX_ROUTE_LENGTH = 512
}
