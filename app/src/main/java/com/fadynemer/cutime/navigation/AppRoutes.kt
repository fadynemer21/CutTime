package com.fadynemer.cutime.navigation

import android.net.Uri

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

    data object BarberProfile :
        AppRoute("barber_profile/{barberId}") {
        fun create(barberId: String) =
            "barber_profile/${Uri.encode(barberId)}"
    }

    data object Booking :
        AppRoute("booking/{barberId}") {
        fun create(barberId: String) =
            "booking/${Uri.encode(barberId)}"
    }

    data object CustomerAppointmentDetail :
        AppRoute("appointment/{appointmentId}") {
        fun create(appointmentId: String) =
            "appointment/${Uri.encode(appointmentId)}"
    }

    data object BarberAppointmentDetail :
        AppRoute("barber_appointment/{appointmentId}") {
        fun create(appointmentId: String) =
            "barber_appointment/${Uri.encode(appointmentId)}"
    }

    data object Reschedule :
        AppRoute("reschedule/{appointmentId}") {
        fun create(appointmentId: String) =
            "reschedule/${Uri.encode(appointmentId)}"
    }

    data object Rating :
        AppRoute("rating/{appointmentId}") {
        fun create(appointmentId: String) =
            "rating/${Uri.encode(appointmentId)}"
    }
}

object RouteArguments {
    const val BARBER_ID = "barberId"
    const val APPOINTMENT_ID = "appointmentId"
}
