package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.AppointmentGroups
import com.fadynemer.cutime.repository.AppointmentAuthenticationException
import com.fadynemer.cutime.repository.AppointmentListDataSource
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.AppointmentRepository
import com.fadynemer.cutime.repository.AppointmentActionsDataSource
import com.fadynemer.cutime.util.AppointmentGrouper
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException

data class AppointmentsUiState(
    val isLoading: Boolean = true,
    val updatingAppointmentId: String? = null,
    val groups: AppointmentGroups = AppointmentGroups(),
    val errorMessage: String? = null
) {
    val isEmpty: Boolean
        get() =
            groups.upcoming.isEmpty() &&
                groups.completed.isEmpty() &&
                groups.cancelled.isEmpty()
}

class AppointmentsViewModel(
    private val appointmentRepository:
        AppointmentListDataSource = AppointmentRepository(),
    private val appointmentActions:
        AppointmentActionsDataSource? =
            appointmentRepository as? AppointmentActionsDataSource
) : ViewModel() {

    var uiState by mutableStateOf(AppointmentsUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observeAppointments()
    }

    fun retry() {
        observation?.stop()
        observation = null

        uiState = AppointmentsUiState(
            isLoading = true
        )

        observeAppointments()
    }

    fun cancelAppointment(
        appointmentId: String
    ) {
        val actions = appointmentActions

        if (actions == null) {
            uiState = uiState.copy(
                errorMessage =
                    "Appointment actions are unavailable."
            )
            return
        }

        if (uiState.updatingAppointmentId != null) return

        uiState = uiState.copy(
            updatingAppointmentId = appointmentId,
            errorMessage = null
        )
        actions.cancelAppointment(appointmentId) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        updatingAppointmentId = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        updatingAppointmentId = null,
                        errorMessage =
                            createReadableError(error)
                    )
                }
        }
    }

    fun deleteFromHistory(
        appointmentId: String
    ) {
        clearHistory(listOf(appointmentId))
    }

    fun clearHistory() {
        clearHistory(
            uiState.groups.completed.map { it.id } +
                uiState.groups.cancelled.map { it.id }
        )
    }

    private fun clearHistory(appointmentIds: List<String>) {
        val actions = appointmentActions

        if (actions == null) {
            uiState = uiState.copy(
                errorMessage =
                    "Appointment actions are unavailable."
            )
            return
        }

        if (uiState.updatingAppointmentId != null) return

        val ids = appointmentIds.distinct()
        if (ids.isEmpty()) return

        uiState = uiState.copy(
            updatingAppointmentId = ids.first(),
            errorMessage = null
        )
        actions.hideCustomerAppointments(
            ids
        ) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        updatingAppointmentId = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        updatingAppointmentId = null,
                        errorMessage =
                            createReadableError(error)
                    )
                }
        }
    }

    private fun observeAppointments() {
        observation =
            appointmentRepository.observeCustomerAppointments { result ->
                result
                    .onSuccess { appointments ->
                        uiState = AppointmentsUiState(
                            isLoading = false,
                            groups =
                                AppointmentGrouper.group(
                                    appointments
                                )
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage =
                                createReadableError(error)
                        )
                    }
            }
    }

    private fun createReadableError(
        error: Throwable
    ): String {
        return when {
            error is AppointmentAuthenticationException ->
                error.message
                    ?: "Please log in again to continue."

            error is FirebaseNetworkException ->
                "Check your internet connection and try again."

            error is FirebaseFirestoreException &&
                error.code ==
                FirebaseFirestoreException.Code.FAILED_PRECONDITION ->
                "A Firestore index is required. Follow the setup instructions."

            error is FirebaseFirestoreException &&
                error.code ==
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                "Appointments cannot be loaded until the Firebase rules are updated."

            else ->
                error.localizedMessage
                    ?: "Appointments could not be loaded. Please try again."
        }
    }

    override fun onCleared() {
        observation?.stop()
        observation = null
        super.onCleared()
    }
}
