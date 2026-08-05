package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.AppointmentRepository
import com.fadynemer.cutime.repository.BarberAppointmentDataSource
import com.fadynemer.cutime.repository.AppointmentActionsDataSource

data class BarberAppointmentHistoryUiState(
    val isLoading: Boolean = true,
    val isClearing: Boolean = false,
    val appointments: List<Appointment> = emptyList(),
    val errorMessage: String? = null,
    val useGenericErrorMessage: Boolean = false
) {
    val isEmpty: Boolean
        get() = appointments.isEmpty()
}

class BarberAppointmentHistoryViewModel(
    private val repository: BarberAppointmentDataSource =
        AppointmentRepository(),
    private val actions: AppointmentActionsDataSource? =
        repository as? AppointmentActionsDataSource
) : ViewModel() {
    var uiState by mutableStateOf(BarberAppointmentHistoryUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observe()
    }

    fun retry() {
        observation?.stop()
        observation = null
        uiState = BarberAppointmentHistoryUiState()
        observe()
    }

    fun deleteFromHistory(appointmentId: String) {
        clear(listOf(appointmentId))
    }

    fun clearHistory() {
        clear(uiState.appointments.map(Appointment::id))
    }

    private fun clear(ids: List<String>) {
        val historyActions = actions
        if (historyActions == null) {
            uiState = uiState.copy(
                errorMessage = "Appointment actions are unavailable."
            )
            return
        }
        if (uiState.isClearing || ids.isEmpty()) return

        uiState = uiState.copy(
            isClearing = true,
            errorMessage = null
        )
        historyActions.hideBarberAppointments(ids.distinct()) { result ->
            result.onSuccess {
                uiState = uiState.copy(isClearing = false)
            }.onFailure { error ->
                uiState = uiState.copy(
                    isClearing = false,
                    errorMessage = error.localizedMessage
                        ?: "History could not be cleared."
                )
            }
        }
    }

    private fun observe() {
        observation = repository.observeBarberAppointments { result ->
            result.onSuccess { appointments ->
                uiState = BarberAppointmentHistoryUiState(
                    isLoading = false,
                    appointments = appointments
                        .filter {
                            it.status == AppointmentStatus.COMPLETED ||
                                it.status == AppointmentStatus.CANCELLED
                        }
                        .sortedByDescending(Appointment::startAtMillis)
                )
            }.onFailure { error ->
                uiState = BarberAppointmentHistoryUiState(
                    isLoading = false,
                    errorMessage = error.localizedMessage,
                    useGenericErrorMessage =
                        error.localizedMessage.isNullOrBlank()
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
