package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberCatalogDataSource
import com.fadynemer.cutime.repository.BarberCatalogRepository

data class BookingAvailabilityUiState(
    val isLoading: Boolean = false,
    val barberId: String? = null,
    val date: String? = null,
    val occupiedTimes: Set<String> = emptySet(),
    val errorMessage: String? = null
)

class BookingAvailabilityViewModel(
    private val repository:
        BarberCatalogDataSource = BarberCatalogRepository()
) : ViewModel() {

    var uiState by mutableStateOf(BookingAvailabilityUiState())
        private set

    private var observation: AppointmentObservation? = null

    fun observe(
        barberId: String,
        date: String
    ) {
        if (
            uiState.barberId == barberId &&
            uiState.date == date
        ) {
            return
        }

        observation?.stop()
        uiState = BookingAvailabilityUiState(
            isLoading = true,
            barberId = barberId,
            date = date
        )
        observation =
            repository.observeOccupiedTimes(
                barberId = barberId,
                date = date
            ) { result ->
                result
                    .onSuccess { times ->
                        uiState = uiState.copy(
                            isLoading = false,
                            occupiedTimes = times,
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            occupiedTimes = emptySet(),
                            errorMessage =
                                error.localizedMessage
                                    ?: "Availability could not be loaded."
                        )
                    }
            }
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}
