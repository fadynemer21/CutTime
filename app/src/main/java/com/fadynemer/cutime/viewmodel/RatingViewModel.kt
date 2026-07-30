package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.Rating
import com.fadynemer.cutime.model.RatingRequest
import com.fadynemer.cutime.repository.AppointmentActionsDataSource
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.AppointmentRepository
import com.fadynemer.cutime.repository.RatingDataSource
import com.fadynemer.cutime.repository.RatingRepository

data class RatingUiState(
    val isLoading: Boolean = true,
    val appointment: Appointment? = null,
    val existingRating: Rating? = null,
    val stars: Int = 0,
    val review: String = "",
    val isSubmitting: Boolean = false,
    val isSuccessful: Boolean = false,
    val errorMessage: String? = null
)

class RatingViewModel(
    private val appointmentRepository:
        AppointmentActionsDataSource = AppointmentRepository(),
    private val ratingRepository:
        RatingDataSource = RatingRepository()
) : ViewModel() {

    var uiState by mutableStateOf(RatingUiState())
        private set

    private var appointmentObservation:
        AppointmentObservation? = null
    private var ratingObservation:
        AppointmentObservation? = null

    fun observe(appointmentId: String) {
        appointmentObservation?.stop()
        ratingObservation?.stop()
        uiState = RatingUiState()

        appointmentObservation =
            appointmentRepository.observeAppointment(
                appointmentId
            ) { result ->
                result
                    .onSuccess { appointment ->
                        uiState = uiState.copy(
                            isLoading = false,
                            appointment = appointment,
                            errorMessage =
                                if (appointment == null) {
                                    "Appointment not found."
                                } else {
                                    null
                                }
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage =
                                error.localizedMessage
                        )
                    }
            }

        ratingObservation =
            ratingRepository.observeAppointmentRating(
                appointmentId
            ) { result ->
                result.onSuccess { rating ->
                    uiState = uiState.copy(
                        existingRating = rating
                    )
                }
            }
    }

    fun selectStars(stars: Int) {
        uiState = uiState.copy(
            stars = stars.coerceIn(1, 5),
            errorMessage = null
        )
    }

    fun updateReview(review: String) {
        uiState = uiState.copy(
            review = review.take(500),
            errorMessage = null
        )
    }

    fun submit() {
        val appointment =
            uiState.appointment ?: return

        if (
            uiState.isSubmitting ||
            uiState.existingRating != null
        ) {
            return
        }

        uiState = uiState.copy(
            isSubmitting = true,
            errorMessage = null
        )
        ratingRepository.submitRating(
            RatingRequest(
                appointmentId = appointment.id,
                barberId = appointment.barberId,
                stars = uiState.stars,
                review = uiState.review
            )
        ) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        isSuccessful = true
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Rating could not be submitted."
                    )
                }
        }
    }

    override fun onCleared() {
        appointmentObservation?.stop()
        ratingObservation?.stop()
        super.onCleared()
    }
}
