package com.fadynemer.cutime.util

/**
 * Stable semantics identifiers for accessibility tooling and device tests.
 * They deliberately describe user intent rather than implementation details.
 */
object UiTestTags {
    const val HOME_SEARCH = "home_search"
    const val HOME_CATALOG = "home_catalog"
    const val HOME_RETRY = "home_retry"
    const val APPOINTMENTS_CONTENT = "appointments_content"
    const val APPOINTMENTS_RETRY = "appointments_retry"
    const val APPOINTMENT_CARD_PREFIX = "appointment_card_"
    const val CANCEL_DIALOG = "cancel_appointment_dialog"
    const val DELETE_HISTORY_DIALOG = "delete_history_dialog"
    const val BOOKING_FORM = "booking_form"
    const val BOOKING_REVIEW = "booking_review"
    const val BOOKING_SUBMIT = "booking_submit"
    const val BOOKING_SUCCESS = "booking_success"
    const val SERVICE_OPTION_PREFIX = "service_option_"
    const val DATE_OPTION_PREFIX = "date_option_"
    const val TIME_OPTION_PREFIX = "time_option_"
}
