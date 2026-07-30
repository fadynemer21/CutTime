package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.repository.AppointmentActionsDataSource
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.AppointmentRepository

data class AppointmentDetailUiState(
    val isLoading: Boolean = true,
    val appointment: Appointment? = null,
    val isUpdating: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AppointmentDetailViewModel(
    private val repository:
        AppointmentActionsDataSource = AppointmentRepository()
) : ViewModel() {

    var uiState by mutableStateOf(AppointmentDetailUiState())
        private set

    private var observation: AppointmentObservation? = null
    private var observedId: String? = null

    fun observe(appointmentId: String) {
        if (observedId == appointmentId) return

        observation?.stop()
        observedId = appointmentId
        uiState = AppointmentDetailUiState()
        observation =
            repository.observeAppointment(appointmentId) { result ->
                result
                    .onSuccess { appointment ->
                        uiState = uiState.copy(
                            isLoading = false,
                            appointment = appointment,
                            errorMessage =
                                if (appointment == null) {
                                    "This appointment no longer exists."
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
                                    ?: "Appointment could not be loaded."
                        )
                    }
            }
    }

    fun cancel() {
        val appointmentId =
            uiState.appointment?.id ?: return
        performAction { callback ->
            repository.cancelAppointment(
                appointmentId,
                callback
            )
        }
    }

    fun complete() {
        val appointmentId =
            uiState.appointment?.id ?: return
        performAction { callback ->
            repository.completeAppointment(
                appointmentId,
                callback
            )
        }
    }

    private fun performAction(
        action: ((Result<Unit>) -> Unit) -> Unit
    ) {
        if (uiState.isUpdating) return

        uiState = uiState.copy(
            isUpdating = true,
            errorMessage = null,
            successMessage = null
        )
        action { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isUpdating = false,
                        successMessage = "Appointment updated."
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isUpdating = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Appointment could not be updated."
                    )
                }
        }
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}
