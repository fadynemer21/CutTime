package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.model.BookingRequest
import com.fadynemer.cutime.repository.AppointmentAuthenticationException
import com.fadynemer.cutime.repository.AppointmentBookingDataSource
import com.fadynemer.cutime.repository.AppointmentRepository
import com.fadynemer.cutime.repository.BookingConflictException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException

data class BookingUiState(
    val selectedServiceId: String? = null,
    val selectedDate: String? = null,
    val selectedTime: String? = null,
    val isReviewing: Boolean = false,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val createdAppointmentId: String? = null
) {
    val canReview: Boolean
        get() =
            selectedServiceId != null &&
                selectedDate != null &&
                selectedTime != null
}

class BookingViewModel(
    private val appointmentRepository:
        AppointmentBookingDataSource = AppointmentRepository()
) : ViewModel() {

    var uiState by mutableStateOf(BookingUiState())
        private set

    fun selectService(serviceId: String) {
        uiState = uiState.copy(
            selectedServiceId = serviceId,
            isReviewing = false,
            errorMessage = null
        )
    }

    fun selectDate(date: String) {
        uiState = uiState.copy(
            selectedDate = date,
            selectedTime = null,
            isReviewing = false,
            errorMessage = null
        )
    }

    fun selectTime(time: String) {
        uiState = uiState.copy(
            selectedTime = time,
            isReviewing = false,
            errorMessage = null
        )
    }

    fun reviewBooking() {
        if (uiState.canReview) {
            uiState = uiState.copy(
                isReviewing = true
            )
        }
    }

    fun editBooking() {
        if (uiState.isSubmitting) {
            return
        }

        uiState = uiState.copy(
            isReviewing = false,
            errorMessage = null
        )
    }

    fun submitBooking(
        barberShop: BarberShop
    ) {
        if (
            uiState.isSubmitting ||
            !uiState.canReview
        ) {
            return
        }

        val selectedService =
            barberShop.services.find { service ->
                service.id == uiState.selectedServiceId
            }

        if (selectedService == null) {
            uiState = uiState.copy(
                errorMessage =
                    "The selected service is no longer available."
            )
            return
        }

        val request = BookingRequest(
            barberId = barberShop.id,
            barberName = barberShop.name,
            serviceId = selectedService.id,
            serviceName = selectedService.name,
            price = selectedService.price,
            durationMinutes = selectedService.durationMinutes,
            appointmentDate = uiState.selectedDate.orEmpty(),
            appointmentTime = uiState.selectedTime.orEmpty()
        )

        uiState = uiState.copy(
            isSubmitting = true,
            errorMessage = null
        )

        appointmentRepository.createAppointment(
            request = request
        ) { result ->
            result
                .onSuccess { appointmentId ->
                    uiState = uiState.copy(
                        isSubmitting = false,
                        createdAppointmentId = appointmentId,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage =
                            createReadableBookingError(error)
                    )
                }
        }
    }

    fun clearError() {
        uiState = uiState.copy(
            errorMessage = null
        )
    }

    private fun createReadableBookingError(
        error: Throwable
    ): String {
        return when {
            error is BookingConflictException ->
                error.message
                    ?: "That time is no longer available."

            error is AppointmentAuthenticationException ->
                error.message
                    ?: "Please log in again to continue."

            error is FirebaseNetworkException ->
                "Check your internet connection and try again."

            error is FirebaseFirestoreException &&
                error.code ==
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Booking is not permitted. The Firebase rules may need updating."

            error is IllegalArgumentException ->
                error.message
                    ?: "Check the booking details and try again."

            else ->
                error.localizedMessage
                    ?: "The appointment could not be booked. Please try again."
        }
    }
}
