package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.data.SampleBarberData
import com.fadynemer.cutime.model.BookingRequest
import com.fadynemer.cutime.repository.AppointmentBookingDataSource
import com.fadynemer.cutime.repository.BookingConflictException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingViewModelTest {

    @Test
    fun allSelections_enableReview() {
        val viewModel = createViewModel()

        viewModel.selectService("service_1")
        viewModel.selectDate("2026-07-30")
        viewModel.selectTime("14:30")

        assertTrue(viewModel.uiState.canReview)
    }

    @Test
    fun changingDate_clearsPreviouslySelectedTime() {
        val viewModel = createViewModel()
        viewModel.selectDate("2026-07-30")
        viewModel.selectTime("14:30")

        viewModel.selectDate("2026-07-31")

        assertNull(viewModel.uiState.selectedTime)
        assertFalse(viewModel.uiState.canReview)
    }

    @Test
    fun incompleteSelection_cannotEnterReview() {
        val viewModel = createViewModel()
        viewModel.selectService("service_1")

        viewModel.reviewBooking()

        assertFalse(viewModel.uiState.isReviewing)
    }

    @Test
    fun completedSelection_canEnterAndLeaveReview() {
        val viewModel = createViewModel()
        viewModel.selectService("service_1")
        viewModel.selectDate("2026-07-30")
        viewModel.selectTime("14:30")

        viewModel.reviewBooking()
        assertTrue(viewModel.uiState.isReviewing)

        viewModel.editBooking()
        assertFalse(viewModel.uiState.isReviewing)
        assertEquals("service_1", viewModel.uiState.selectedServiceId)
    }

    @Test
    fun successfulSubmission_exposesCreatedAppointmentId() {
        val repository = FakeBookingRepository()
        val viewModel = BookingViewModel(repository)
        val barber = SampleBarberData.barberShops.first()
        selectCompleteBooking(viewModel)

        viewModel.submitBooking(barber)
        repository.completeWithSuccess("appointment_1")

        assertFalse(viewModel.uiState.isSubmitting)
        assertEquals(
            "appointment_1",
            viewModel.uiState.createdAppointmentId
        )
    }

    @Test
    fun conflictingSubmission_exposesReadableError() {
        val repository = FakeBookingRepository()
        val viewModel = BookingViewModel(repository)
        val barber = SampleBarberData.barberShops.first()
        selectCompleteBooking(viewModel)

        viewModel.submitBooking(barber)
        repository.completeWithFailure(
            BookingConflictException()
        )

        assertFalse(viewModel.uiState.isSubmitting)
        assertTrue(
            viewModel.uiState.errorMessage
                ?.contains("choose another time") == true
        )
    }

    private fun createViewModel() =
        BookingViewModel(
            FakeBookingRepository()
        )

    private fun selectCompleteBooking(
        viewModel: BookingViewModel
    ) {
        viewModel.selectService("service_1")
        viewModel.selectDate("2099-07-30")
        viewModel.selectTime("14:30")
        viewModel.reviewBooking()
    }

    private class FakeBookingRepository :
        AppointmentBookingDataSource {

        private var callback:
            ((Result<String>) -> Unit)? = null

        override fun createAppointment(
            request: BookingRequest,
            onResult: (Result<String>) -> Unit
        ) {
            callback = onResult
        }

        fun completeWithSuccess(
            appointmentId: String
        ) {
            callback?.invoke(
                Result.success(appointmentId)
            )
        }

        fun completeWithFailure(
            error: Throwable
        ) {
            callback?.invoke(
                Result.failure(error)
            )
        }
    }
}
