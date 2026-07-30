package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.AvailabilitySaveResult
import com.fadynemer.cutime.model.ManagedBarberProfile
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberServicesViewModelTest {

    @Test
    fun invalidService_doesNotWrite() {
        val repository = FakeBarberDataSource()
        val viewModel = BarberServicesViewModel(repository)

        viewModel.saveService(
            BarberService("", "", 0, 10)
        ) {}

        assertFalse(repository.saveWasCalled)
        assertTrue(viewModel.uiState.errorMessage != null)
    }

    @Test
    fun validService_successClosesEditor() {
        val repository = FakeBarberDataSource()
        val viewModel = BarberServicesViewModel(repository)
        var editorClosed = false

        viewModel.saveService(
            BarberService("", "Haircut", 60, 30)
        ) {
            editorClosed = true
        }

        assertTrue(repository.saveWasCalled)
        assertTrue(editorClosed)
        assertEquals(
            "Service saved.",
            viewModel.uiState.successMessage
        )
    }

    private class FakeBarberDataSource : BarberDataSource {
        var saveWasCalled = false

        override fun observeServices(
            onResult: (Result<List<BarberService>>) -> Unit
        ): AppointmentObservation {
            onResult(Result.success(emptyList()))
            return AppointmentObservation {}
        }

        override fun saveService(
            service: BarberService,
            onResult: (Result<String>) -> Unit
        ) {
            saveWasCalled = true
            onResult(Result.success("service_1"))
        }

        override fun observeProfile(
            onResult:
                (Result<ManagedBarberProfile?>) -> Unit
        ) = AppointmentObservation {}

        override fun saveProfile(
            profile: ManagedBarberProfile,
            onResult: (Result<Unit>) -> Unit
        ) = onResult(Result.success(Unit))

        override fun deleteService(
            serviceId: String,
            onResult: (Result<Unit>) -> Unit
        ) = onResult(Result.success(Unit))

        override fun observeAvailability(
            onResult: (Result<BarberAvailability>) -> Unit
        ) = AppointmentObservation {}

        override fun saveAvailability(
            availability: BarberAvailability,
            onResult: (Result<AvailabilitySaveResult>) -> Unit
        ) = onResult(
            Result.success(AvailabilitySaveResult())
        )
    }
}
