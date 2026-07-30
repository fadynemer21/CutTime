package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.AvailabilitySaveResult
import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.ManagedBarberProfile
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberAvailabilityViewModelTest {

    @Test
    fun successfulHolidaySave_reportsCancelledAppointmentCount() {
        val repository =
            FakeAvailabilityRepository(
                saveResult =
                    Result.success(
                        AvailabilitySaveResult(
                            cancelledAppointmentCount = 2
                        )
                    )
            )
        val viewModel = BarberAvailabilityViewModel(repository)

        viewModel.addBlockedDate("2099-08-12")
        viewModel.save()

        assertTrue(repository.saveWasCalled)
        assertFalse(viewModel.uiState.isSaving)
        assertEquals(
            "Availability saved. 2 appointments were cancelled.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun successfulHolidaySave_usesSingularAppointmentCopy() {
        val repository =
            FakeAvailabilityRepository(
                saveResult =
                    Result.success(
                        AvailabilitySaveResult(
                            cancelledAppointmentCount = 1
                        )
                    )
            )
        val viewModel = BarberAvailabilityViewModel(repository)

        viewModel.addBlockedDate("2099-08-12")
        viewModel.save()

        assertEquals(
            "Availability saved. 1 appointment was cancelled.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun regularAvailabilitySave_keepsSimpleSuccessMessage() {
        val repository =
            FakeAvailabilityRepository(
                saveResult =
                    Result.success(AvailabilitySaveResult())
            )
        val viewModel = BarberAvailabilityViewModel(repository)

        viewModel.save()

        assertEquals(
            "Availability saved.",
            viewModel.uiState.successMessage
        )
    }

    private class FakeAvailabilityRepository(
        private val saveResult:
            Result<AvailabilitySaveResult>
    ) : BarberDataSource {

        var saveWasCalled = false

        override fun observeAvailability(
            onResult: (Result<BarberAvailability>) -> Unit
        ): AppointmentObservation {
            onResult(Result.success(BarberAvailability()))
            return AppointmentObservation {}
        }

        override fun saveAvailability(
            availability: BarberAvailability,
            onResult: (Result<AvailabilitySaveResult>) -> Unit
        ) {
            saveWasCalled = true
            onResult(saveResult)
        }

        override fun observeProfile(
            onResult: (Result<ManagedBarberProfile?>) -> Unit
        ) = AppointmentObservation {}

        override fun saveProfile(
            profile: ManagedBarberProfile,
            onResult: (Result<Unit>) -> Unit
        ) = onResult(Result.success(Unit))

        override fun observeServices(
            onResult: (Result<List<BarberService>>) -> Unit
        ) = AppointmentObservation {}

        override fun saveService(
            service: BarberService,
            onResult: (Result<String>) -> Unit
        ) = onResult(Result.success(service.id))

        override fun deleteService(
            serviceId: String,
            onResult: (Result<Unit>) -> Unit
        ) = onResult(Result.success(Unit))
    }
}
