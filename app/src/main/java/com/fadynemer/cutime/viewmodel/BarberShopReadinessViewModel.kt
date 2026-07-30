package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.BarberShopReadiness
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberShopReadinessDataSource
import com.fadynemer.cutime.repository.BarberShopReadinessRepository

data class BarberShopReadinessUiState(
    val isLoading: Boolean = true,
    val readiness: BarberShopReadiness =
        BarberShopReadiness(),
    val errorMessage: String? = null
)

class BarberShopReadinessViewModel(
    private val repository:
        BarberShopReadinessDataSource =
            BarberShopReadinessRepository()
) : ViewModel() {

    var uiState by mutableStateOf(
        BarberShopReadinessUiState()
    )
        private set

    private var observation: AppointmentObservation? = null

    init {
        observe()
    }

    fun retry() {
        observation?.stop()
        observation = null
        uiState = BarberShopReadinessUiState()
        observe()
    }

    private fun observe() {
        observation =
            repository.observeReadiness { result ->
                result
                    .onSuccess { readiness ->
                        uiState = BarberShopReadinessUiState(
                            isLoading = false,
                            readiness = readiness
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage =
                                error.localizedMessage
                                    ?: "Shop readiness could not be checked."
                        )
                    }
            }
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}
