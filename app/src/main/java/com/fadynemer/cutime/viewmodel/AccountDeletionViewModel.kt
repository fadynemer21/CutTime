package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.AccountDeletionRequest
import com.fadynemer.cutime.repository.AccountDeletionDataSource
import com.fadynemer.cutime.repository.AccountDeletionRepository
import com.fadynemer.cutime.repository.AppointmentObservation

data class AccountDeletionUiState(
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val showConfirmation: Boolean = false,
    val request: AccountDeletionRequest? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AccountDeletionViewModel(
    private val repository: AccountDeletionDataSource =
        AccountDeletionRepository()
) : ViewModel() {

    var uiState by mutableStateOf(AccountDeletionUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observe()
    }

    fun showConfirmation() {
        if (uiState.request != null || uiState.isSubmitting) return
        uiState = uiState.copy(
            showConfirmation = true,
            errorMessage = null,
            successMessage = null
        )
    }

    fun dismissConfirmation() {
        if (uiState.isSubmitting) return
        uiState = uiState.copy(showConfirmation = false)
    }

    fun submit(role: String) {
        if (uiState.isSubmitting || uiState.request != null) return
        uiState = uiState.copy(
            isSubmitting = true,
            errorMessage = null,
            successMessage = null
        )
        repository.submitRequest(role) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isSubmitting = false,
                        showConfirmation = false,
                        successMessage =
                            "Account deletion request submitted."
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSubmitting = false,
                        errorMessage = error.localizedMessage
                            ?: "The deletion request could not be submitted."
                    )
                }
        }
    }

    private fun observe() {
        observation?.stop()
        observation = repository.observeRequest { result ->
            result
                .onSuccess { request ->
                    uiState = uiState.copy(
                        isLoading = false,
                        request = request,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage
                            ?: "Deletion request status could not be loaded."
                    )
                }
        }
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}
