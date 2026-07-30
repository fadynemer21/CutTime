package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.repository.BookingConflictException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RescheduleViewModelTest {
    @Test
    fun observe_startsLoadingRequestedAppointment() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = RescheduleViewModel(repository)

        viewModel.observe("appointment_12")

        assertEquals(
            "appointment_12",
            repository.observedAppointmentId
        )
        assertTrue(viewModel.uiState.isLoading)
    }

    @Test
    fun successfulObservation_exposesAppointment() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = RescheduleViewModel(repository)
        val appointment = testAppointment()
        viewModel.observe(appointment.id)

        repository.emitAppointment(Result.success(appointment))

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(appointment, viewModel.uiState.appointment)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun missingAppointment_exposesNotFoundMessage() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = RescheduleViewModel(repository)
        viewModel.observe("missing")

        repository.emitAppointment(Result.success(null))

        assertEquals(
            "Appointment not found.",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun failedObservation_exposesRepositoryMessage() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = RescheduleViewModel(repository)
        viewModel.observe("appointment_1")

        repository.emitAppointment(
            Result.failure(Exception("Read denied"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Read denied",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun selectingDate_clearsPreviouslySelectedTime() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.selectDate("2099-12-30")
        viewModel.selectTime("11:00")

        viewModel.selectDate("2099-12-31")

        assertEquals(
            "2099-12-31",
            viewModel.uiState.selectedDate
        )
        assertNull(viewModel.uiState.selectedTime)
    }

    @Test
    fun selectingTime_clearsError() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = RescheduleViewModel(repository)
        viewModel.observe("appointment_1")
        repository.emitAppointment(
            Result.failure(Exception("Temporary error"))
        )

        viewModel.selectTime("12:30")

        assertEquals("12:30", viewModel.uiState.selectedTime)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun canSubmit_requiresAppointmentDateAndTime() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)

        assertFalse(viewModel.uiState.canSubmit)
        viewModel.selectDate("2099-12-31")
        assertFalse(viewModel.uiState.canSubmit)
        viewModel.selectTime("15:00")
        assertTrue(viewModel.uiState.canSubmit)
    }

    @Test
    fun submitWithoutDate_isIgnored() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.selectTime("15:00")

        viewModel.submit()

        assertEquals(0, repository.rescheduleCalls)
    }

    @Test
    fun submitWithoutTime_isIgnored() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.selectDate("2099-12-31")

        viewModel.submit()

        assertEquals(0, repository.rescheduleCalls)
    }

    @Test
    fun submitBuildsExpectedRequest() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.selectDate("2099-12-31")
        viewModel.selectTime("15:00")

        viewModel.submit()

        assertTrue(viewModel.uiState.isSubmitting)
        assertEquals(
            "appointment_1",
            repository.rescheduleRequest?.appointmentId
        )
        assertEquals(
            "2099-12-31",
            repository.rescheduleRequest?.appointmentDate
        )
        assertEquals(
            "15:00",
            repository.rescheduleRequest?.appointmentTime
        )
    }

    @Test
    fun successfulSubmit_marksFlowSuccessful() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = readyViewModel(repository)

        viewModel.submit()
        repository.completeReschedule(Result.success(Unit))

        assertFalse(viewModel.uiState.isSubmitting)
        assertTrue(viewModel.uiState.isSuccessful)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun bookingConflict_hasPurposeBuiltMessage() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = readyViewModel(repository)

        viewModel.submit()
        repository.completeReschedule(
            Result.failure(BookingConflictException())
        )

        assertFalse(viewModel.uiState.isSubmitting)
        assertEquals(
            "That time is no longer available.",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun genericFailure_usesRepositoryMessage() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = readyViewModel(repository)

        viewModel.submit()
        repository.completeReschedule(
            Result.failure(Exception("Network lost"))
        )

        assertEquals(
            "Network lost",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun duplicateSubmitWhilePending_isIgnored() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = readyViewModel(repository)

        viewModel.submit()
        viewModel.submit()

        assertEquals(1, repository.rescheduleCalls)
    }

    @Test
    fun observingSameAppointmentTwice_keepsCurrentListener() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = RescheduleViewModel(repository)

        viewModel.observe("appointment_1")
        viewModel.observe("appointment_1")

        assertEquals(0, repository.observationStopCount)
    }

    @Test
    fun observingDifferentAppointment_stopsPreviousListener() {
        val repository = FakeAppointmentActionsDataSource()
        val viewModel = RescheduleViewModel(repository)

        viewModel.observe("appointment_1")
        viewModel.observe("appointment_2")

        assertEquals(1, repository.observationStopCount)
        assertEquals(
            "appointment_2",
            repository.observedAppointmentId
        )
    }

    private fun loadedViewModel(
        repository: FakeAppointmentActionsDataSource
    ): RescheduleViewModel {
        return RescheduleViewModel(repository).also {
            it.observe("appointment_1")
            repository.emitAppointment(
                Result.success(testAppointment())
            )
        }
    }

    private fun readyViewModel(
        repository: FakeAppointmentActionsDataSource
    ): RescheduleViewModel {
        return loadedViewModel(repository).also {
            it.selectDate("2099-12-31")
            it.selectTime("15:00")
        }
    }
}
