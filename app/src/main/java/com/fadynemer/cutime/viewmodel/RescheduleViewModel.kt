package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.RescheduleRequest
import com.fadynemer.cutime.repository.AppointmentActionsDataSource
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.AppointmentRepository
import com.fadynemer.cutime.repository.BookingConflictException

data class RescheduleUiState(
    val isLoading: Boolean = true,
    val appointment: Appointment? = null,
    val selectedDate: String? = null,
    val selectedTime: String? = null,
    val isSubmitting: Boolean = false,
    val isSuccessful: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() =
            appointment != null &&
                selectedDate != null &&
                selectedTime != null &&
                !isSubmitting
}

class RescheduleViewModel(
    private val repository:
        AppointmentActionsDataSource = AppointmentRepository()
) : ViewModel() {

    var uiState by mutableStateOf(RescheduleUiState())
        private set

    private var observation: AppointmentObservation? = null
    private var observedId: String? = null

    fun observe(appointmentId: String) {
        if (observedId == appointmentId) return

        observedId = appointmentId
        observation?.stop()
        observation =
            repository.observeAppointment(appointmentId) { result ->
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
                                    ?: "Appointment could not be loaded."
                        )
                    }
            }
    }

    fun selectDate(date: String) {
        uiState = uiState.copy(
            selectedDate = date,
            selectedTime = null,
            errorMessage = null
        )
    }

    fun selectTime(time: String) {
        uiState = uiState.copy(
            selectedTime = time,
            errorMessage = null
        )
    }

    fun submit() {
        val appointment =
            uiState.appointment ?: return
        val date =
            uiState.selectedDate ?: return
        val time =
            uiState.selectedTime ?: return

        if (uiState.isSubmitting) return

        uiState = uiState.copy(
            isSubmitting = true,
            errorMessage = null
        )
        repository.rescheduleAppointment(
            RescheduleRequest(
                appointmentId = appointment.id,
                appointmentDate = date,
                appointmentTime = time
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
                            if (error is BookingConflictException) {
                                "That time is no longer available."
                            } else {
                                error.localizedMessage
                                    ?: "Appointment could not be rescheduled."
                            }
                    )
                }
        }
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}
