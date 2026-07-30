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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class BarberDashboardUiState(
    val isLoading: Boolean = true,
    val today: List<Appointment> = emptyList(),
    val upcoming: List<Appointment> = emptyList(),
    val updatingAppointmentId: String? = null,
    val errorMessage: String? = null
)

class BarberDashboardViewModel(
    private val repository:
        BarberAppointmentDataSource = AppointmentRepository(),
    private val actions:
        AppointmentActionsDataSource? =
            repository as? AppointmentActionsDataSource
) : ViewModel() {

    var uiState by mutableStateOf(BarberDashboardUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observe()
    }

    fun retry() {
        observation?.stop()
        uiState = BarberDashboardUiState()
        observe()
    }

    fun completeAppointment(appointmentId: String) {
        updateAppointment(appointmentId, isCompletion = true)
    }

    fun cancelAppointment(appointmentId: String) {
        updateAppointment(appointmentId, isCompletion = false)
    }

    private fun updateAppointment(
        appointmentId: String,
        isCompletion: Boolean
    ) {
        val appointmentActions = actions ?: return

        if (uiState.updatingAppointmentId != null) return

        uiState = uiState.copy(
            updatingAppointmentId = appointmentId,
            errorMessage = null
        )
        val callback: (Result<Unit>) -> Unit = { result ->
            result.onFailure { error ->
                uiState = uiState.copy(
                    updatingAppointmentId = null,
                    errorMessage =
                        error.localizedMessage
                            ?: "Appointment could not be updated."
                )
            }.onSuccess {
                uiState = uiState.copy(
                    updatingAppointmentId = null
                )
            }
        }

        if (isCompletion) {
            appointmentActions.completeAppointment(
                appointmentId,
                callback
            )
        } else {
            appointmentActions.cancelAppointment(
                appointmentId,
                callback
            )
        }
    }

    private fun observe() {
        observation =
            repository.observeBarberAppointments { result ->
                result
                    .onSuccess(::showAppointments)
                    .onFailure { error ->
                        uiState = BarberDashboardUiState(
                            isLoading = false,
                            errorMessage =
                                error.localizedMessage
                                    ?: "Appointments could not be loaded."
                        )
                    }
            }
    }

    private fun showAppointments(
        appointments: List<Appointment>
    ) {
        val today = LocalDate.now()
        val active =
            appointments.filter { appointment ->
                appointment.status == AppointmentStatus.UPCOMING
            }

        uiState = BarberDashboardUiState(
            isLoading = false,
            today =
                active.filter { appointment ->
                    appointment.localDate() == today
                },
            upcoming =
                active.filter { appointment ->
                    appointment.localDate().isAfter(today)
                }
        )
    }

    private fun Appointment.localDate(): LocalDate {
        return Instant
            .ofEpochMilli(startAtMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}
