package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.model.RescheduleRequest
import com.fadynemer.cutime.repository.AppointmentListDataSource
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.AppointmentActionsDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentsViewModelTest {

    @Test
    fun successfulObservation_groupsAppointments() {
        val repository = FakeAppointmentListDataSource()
        val viewModel = AppointmentsViewModel(repository)

        repository.emit(
            Result.success(
                listOf(
                    appointment(
                        id = "upcoming",
                        startAtMillis =
                            System.currentTimeMillis() + 60_000L,
                        endAtMillis =
                            System.currentTimeMillis() + 120_000L
                    )
                )
            )
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            listOf("upcoming"),
            viewModel.uiState.groups.upcoming.map { it.id }
        )
    }

    @Test
    fun failedObservation_exposesErrorAndRetryRestartsListener() {
        val repository = FakeAppointmentListDataSource()
        val viewModel = AppointmentsViewModel(repository)

        repository.emit(
            Result.failure(
                Exception("Load failed")
            )
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Load failed",
            viewModel.uiState.errorMessage
        )

        viewModel.retry()

        assertTrue(repository.stopWasCalled)
        assertTrue(viewModel.uiState.isLoading)
        assertEquals(2, repository.observationCount)
    }

    @Test
    fun cancellation_usesAppointmentActionRepository() {
        val repository = FakeAppointmentListDataSource()
        val viewModel = AppointmentsViewModel(repository)

        viewModel.cancelAppointment("appointment_1")

        assertEquals(
            "appointment_1",
            repository.cancelledAppointmentId
        )
        assertEquals(
            null,
            viewModel.uiState.updatingAppointmentId
        )
    }

    @Test
    fun deletingCancelledHistory_usesAppointmentActionRepository() {
        val repository = FakeAppointmentListDataSource()
        val viewModel = AppointmentsViewModel(repository)

        viewModel.deleteCancelledFromHistory("cancelled_1")

        assertEquals(
            "cancelled_1",
            repository.hiddenAppointmentId
        )
        assertEquals(
            null,
            viewModel.uiState.updatingAppointmentId
        )
    }

    private class FakeAppointmentListDataSource :
        AppointmentListDataSource,
        AppointmentActionsDataSource {

        private var callback:
            ((Result<List<Appointment>>) -> Unit)? = null

        var stopWasCalled = false
            private set

        var observationCount = 0
            private set

        var cancelledAppointmentId: String? = null
            private set

        var hiddenAppointmentId: String? = null
            private set

        override fun observeCustomerAppointments(
            onResult: (Result<List<Appointment>>) -> Unit
        ): AppointmentObservation {
            callback = onResult
            observationCount += 1

            return AppointmentObservation {
                stopWasCalled = true
            }
        }

        fun emit(
            result: Result<List<Appointment>>
        ) {
            callback?.invoke(result)
        }

        override fun cancelAppointment(
            appointmentId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            cancelledAppointmentId = appointmentId
            onResult(Result.success(Unit))
        }

        override fun completeAppointment(
            appointmentId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            onResult(Result.success(Unit))
        }

        override fun hideCancelledAppointment(
            appointmentId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            hiddenAppointmentId = appointmentId
            onResult(Result.success(Unit))
        }

        override fun observeAppointment(
            appointmentId: String,
            onResult: (Result<Appointment?>) -> Unit
        ): AppointmentObservation {
            onResult(Result.success(null))
            return AppointmentObservation {}
        }

        override fun rescheduleAppointment(
            request: RescheduleRequest,
            onResult: (Result<Unit>) -> Unit
        ) {
            onResult(Result.success(Unit))
        }
    }

    private fun appointment(
        id: String,
        startAtMillis: Long,
        endAtMillis: Long
    ) = Appointment(
        id = id,
        customerId = "customer_1",
        barberId = "barber_1",
        barberName = "Urban Fade Studio",
        serviceId = "service_1",
        serviceName = "Classic Haircut",
        price = 60,
        durationMinutes = 30,
        appointmentDate = "2099-07-30",
        appointmentTime = "14:30",
        startAtMillis = startAtMillis,
        endAtMillis = endAtMillis,
        status = AppointmentStatus.UPCOMING
    )
}
