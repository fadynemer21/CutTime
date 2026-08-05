package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.Rating
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.RatingDataSource
import com.fadynemer.cutime.repository.RatingRepository

data class BarberReviewsUiState(
    val barberId: String? = null,
    val isLoading: Boolean = false,
    val ratings: List<Rating> = emptyList(),
    val deletingRatingId: String? = null,
    val actionErrorMessage: String? = null,
    val errorMessage: String? = null
)

class BarberReviewsViewModel(
    private val repository: RatingDataSource = RatingRepository()
) : ViewModel() {
    var uiState by mutableStateOf(BarberReviewsUiState())
        private set

    private var observation: AppointmentObservation? = null

    fun observe(barberId: String) {
        if (barberId.isBlank()) {
            uiState = BarberReviewsUiState(
                errorMessage = "The barber profile is invalid."
            )
            return
        }

        if (
            uiState.barberId == barberId &&
            observation != null
        ) {
            return
        }

        observation?.stop()
        uiState = BarberReviewsUiState(
            barberId = barberId,
            isLoading = true
        )
        observation = repository.observeBarberRatings(
            barberId
        ) { result ->
            result
                .onSuccess { ratings ->
                    uiState = uiState.copy(
                        isLoading = false,
                        ratings = ratings,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Reviews could not be loaded."
                    )
                }
        }
    }

    fun retry() {
        val barberId = uiState.barberId ?: return
        observation?.stop()
        observation = null
        observe(barberId)
    }

    fun deleteReview(appointmentId: String) {
        if (uiState.deletingRatingId != null) return

        uiState = uiState.copy(
            deletingRatingId = appointmentId,
            actionErrorMessage = null
        )
        repository.deleteRating(appointmentId) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        deletingRatingId = null,
                        actionErrorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        deletingRatingId = null,
                        actionErrorMessage =
                            error.localizedMessage
                                ?: "Review could not be deleted."
                    )
                }
        }
    }

    override fun onCleared() {
        observation?.stop()
        observation = null
        super.onCleared()
    }
}
