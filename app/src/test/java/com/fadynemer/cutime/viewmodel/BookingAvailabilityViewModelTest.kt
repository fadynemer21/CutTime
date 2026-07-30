package com.fadynemer.cutime.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookingAvailabilityViewModelTest {
    @Test
    fun observe_startsListenerForExactBarberAndDate() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = BookingAvailabilityViewModel(repository)

        viewModel.observe("barber_1", "2099-12-31")

        assertTrue(viewModel.uiState.isLoading)
        assertEquals("barber_1", repository.observedBarberId)
        assertEquals("2099-12-31", repository.observedDate)
    }

    @Test
    fun occupiedEmission_updatesState() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = BookingAvailabilityViewModel(repository)
        viewModel.observe("barber_1", "2099-12-31")

        repository.emitOccupied(
            Result.success(setOf("10:00", "10:15"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            setOf("10:00", "10:15"),
            viewModel.uiState.occupiedTimes
        )
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun listenerFailure_clearsPossiblyStaleOccupiedTimes() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = BookingAvailabilityViewModel(repository)
        viewModel.observe("barber_1", "2099-12-31")
        repository.emitOccupied(
            Result.success(setOf("10:00"))
        )

        repository.emitOccupied(
            Result.failure(Exception("Query denied"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertTrue(viewModel.uiState.occupiedTimes.isEmpty())
        assertEquals(
            "Query denied",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun sameBarberAndDate_doesNotRestartListener() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = BookingAvailabilityViewModel(repository)

        viewModel.observe("barber_1", "2099-12-31")
        viewModel.observe("barber_1", "2099-12-31")

        assertEquals(1, repository.occupiedObserveCalls)
        assertEquals(0, repository.occupiedStopCalls)
    }

    @Test
    fun newDate_stopsOldListenerAndClearsOldTimes() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = BookingAvailabilityViewModel(repository)
        viewModel.observe("barber_1", "2099-12-30")
        repository.emitOccupied(
            Result.success(setOf("10:00"))
        )

        viewModel.observe("barber_1", "2099-12-31")

        assertEquals(1, repository.occupiedStopCalls)
        assertEquals(2, repository.occupiedObserveCalls)
        assertTrue(viewModel.uiState.isLoading)
        assertTrue(viewModel.uiState.occupiedTimes.isEmpty())
    }

    @Test
    fun newBarber_stopsOldListener() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = BookingAvailabilityViewModel(repository)
        viewModel.observe("barber_1", "2099-12-31")

        viewModel.observe("barber_2", "2099-12-31")

        assertEquals(1, repository.occupiedStopCalls)
        assertEquals("barber_2", repository.observedBarberId)
    }
}
