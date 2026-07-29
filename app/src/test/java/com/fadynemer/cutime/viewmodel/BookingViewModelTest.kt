package com.fadynemer.cutime.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingViewModelTest {

    @Test
    fun allSelections_enableReview() {
        val viewModel = BookingViewModel()

        viewModel.selectService("service_1")
        viewModel.selectDate("Thursday, 30 July 2026")
        viewModel.selectTime("14:30")

        assertTrue(viewModel.uiState.canReview)
    }

    @Test
    fun changingDate_clearsPreviouslySelectedTime() {
        val viewModel = BookingViewModel()
        viewModel.selectDate("Thursday, 30 July 2026")
        viewModel.selectTime("14:30")

        viewModel.selectDate("Friday, 31 July 2026")

        assertNull(viewModel.uiState.selectedTime)
        assertFalse(viewModel.uiState.canReview)
    }

    @Test
    fun incompleteSelection_cannotEnterReview() {
        val viewModel = BookingViewModel()
        viewModel.selectService("service_1")

        viewModel.reviewBooking()

        assertFalse(viewModel.uiState.isReviewing)
    }

    @Test
    fun completedSelection_canEnterAndLeaveReview() {
        val viewModel = BookingViewModel()
        viewModel.selectService("service_1")
        viewModel.selectDate("Thursday, 30 July 2026")
        viewModel.selectTime("14:30")

        viewModel.reviewBooking()
        assertTrue(viewModel.uiState.isReviewing)

        viewModel.editBooking()
        assertFalse(viewModel.uiState.isReviewing)
        assertEquals("service_1", viewModel.uiState.selectedServiceId)
    }
}
