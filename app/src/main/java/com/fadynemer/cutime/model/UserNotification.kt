package com.fadynemer.cutime.model

data class UserNotification(
    val id: String,
    val userId: String,
    val type: NotificationType,
    val title: String,
    val message: String,
    val appointmentId: String? = null,
    val barberId: String? = null,
    val isRead: Boolean = false,
    val createdAtMillis: Long = 0L
)

enum class NotificationType {
    APPOINTMENT_BOOKED,
    APPOINTMENT_CANCELLED,
    APPOINTMENT_RESCHEDULED,
    APPOINTMENT_COMPLETED,
    APPOINTMENT_REMINDER,
    REVIEW_REQUEST,
    GENERAL;

    companion object {
        fun fromFirestore(value: String?): NotificationType {
            return entries.firstOrNull { it.name == value } ?: GENERAL
        }
    }
}

data class NotificationPreferences(
    val pushEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val appointmentUpdatesEnabled: Boolean = true,
    val reviewPromptsEnabled: Boolean = true
)

data class DeviceRegistration(
    val token: String,
    val platform: String = "ANDROID",
    val appVersion: String,
    val deviceModel: String
)

object NotificationPreferenceLimits {
    /**
     * Every eligible appointment receives both reminders. The order is
     * intentional: the earlier two-hour reminder is scheduled first.
     */
    val REMINDER_LEAD_MINUTES = listOf(
        120,
        30
    )
}
