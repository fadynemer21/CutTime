package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.NotificationType
import com.fadynemer.cutime.model.UserNotification
import com.fadynemer.cutime.navigation.AppRoute

object NotificationRouter {
    fun destination(
        notification: UserNotification,
        isBarberMode: Boolean
    ): String {
        val appointmentId = notification.appointmentId

        if (appointmentId != null) {
            return if (isBarberMode) {
                AppRoute.BarberAppointmentDetail.create(
                    appointmentId
                )
            } else {
                AppRoute.CustomerAppointmentDetail.create(
                    appointmentId
                )
            }
        }

        return when {
            notification.type == NotificationType.GENERAL &&
                notification.barberId != null ->
                AppRoute.BarberProfile.create(
                    notification.barberId
                )

            isBarberMode -> AppRoute.BarberDashboard.pattern
            else -> AppRoute.CustomerHome.pattern
        }
    }

    fun destinationFromPayload(
        data: Map<String, String>
    ): String? {
        val explicitRoute = data["route"]
            ?.takeIf(String::isNotBlank)

        if (explicitRoute != null) {
            return explicitRoute
        }

        val appointmentId = data["appointmentId"]
            ?.takeIf(String::isNotBlank)
            ?: return null
        val audience = data["audience"].orEmpty()

        return if (audience == "BARBER") {
            AppRoute.BarberAppointmentDetail.create(appointmentId)
        } else {
            AppRoute.CustomerAppointmentDetail.create(appointmentId)
        }
    }
}
