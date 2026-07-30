package com.fadynemer.cutime.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppointmentDetailViewModelTest {
    @Test
    fun observe_startsRealtimeObservation() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = AppointmentDetailViewModel(repository)

        viewModel.observe("appointment_1")

        assertEquals(
            "appointment_1",
            repository.observedAppointmentId
        )
        assertTrue(viewModel.uiState.isLoading)
    }

    @Test
    fun appointmentEmission_populatesDetailState() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = AppointmentDetailViewModel(repository)
        viewModel.observe("appointment_1")
        val appointment = testAppointment()

        repository.emitAppointment(Result.success(appointment))

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(appointment, viewModel.uiState.appointment)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun missingAppointment_exposesFriendlyMessage() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = AppointmentDetailViewModel(repository)
        viewModel.observe("missing")

        repository.emitAppointment(Result.success(null))

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "This appointment no longer exists.",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun observationFailure_exposesRepositoryMessage() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = AppointmentDetailViewModel(repository)
        viewModel.observe("appointment_1")

        repository.emitAppointment(
            Result.failure(Exception("Listener failed"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Listener failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun observingSameIdTwice_doesNotRestartListener() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = AppointmentDetailViewModel(repository)

        viewModel.observe("appointment_1")
        viewModel.observe("appointment_1")

        assertEquals(0, repository.observationStopCount)
    }

    @Test
    fun observingDifferentId_stopsOldListener() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = AppointmentDetailViewModel(repository)

        viewModel.observe("appointment_1")
        viewModel.observe("appointment_2")

        assertEquals(1, repository.observationStopCount)
        assertEquals(
            "appointment_2",
            repository.observedAppointmentId
        )
    }

    @Test
    fun cancel_usesLoadedAppointmentId() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)

        viewModel.cancel()

        assertTrue(viewModel.uiState.isUpdating)
        assertEquals(
            "appointment_1",
            repository.cancelledAppointmentId
        )
    }

    @Test
    fun cancelWithoutAppointment_isIgnored() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = AppointmentDetailViewModel(repository)

        viewModel.cancel()

        assertEquals(0, repository.cancelCalls)
    }

    @Test
    fun successfulCancel_clearsProgressAndShowsConfirmation() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.cancel()

        repository.completeCancel(Result.success(Unit))

        assertFalse(viewModel.uiState.isUpdating)
        assertEquals(
            "Appointment updated.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun failedCancel_showsError() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.cancel()

        repository.completeCancel(
            Result.failure(Exception("Cannot cancel"))
        )

        assertFalse(viewModel.uiState.isUpdating)
        assertEquals(
            "Cannot cancel",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun complete_usesLoadedAppointmentId() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)

        viewModel.complete()

        assertTrue(viewModel.uiState.isUpdating)
        assertEquals(
            "appointment_1",
            repository.completedAppointmentId
        )
    }

    @Test
    fun successfulComplete_showsConfirmation() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.complete()

        repository.completeComplete(Result.success(Unit))

        assertFalse(viewModel.uiState.isUpdating)
        assertEquals(
            "Appointment updated.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun duplicateActionWhilePending_isIgnored() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)

        viewModel.cancel()
        viewModel.cancel()
        viewModel.complete()

        assertEquals(1, repository.cancelCalls)
        assertEquals(0, repository.completeCalls)
    }

    private fun loadedViewModel(
        repository: FakeAppointmentActionsDataSource
    ): AppointmentDetailViewModel {
        return AppointmentDetailViewModel(repository).also {
            it.observe("appointment_1")
            repository.emitAppointment(
                Result.success(testAppointment())
            )
        }
    }
}
