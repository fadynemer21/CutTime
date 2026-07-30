package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.BarberShopReadiness
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberShopReadinessDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberShopReadinessViewModelTest {
    private val repository = FakeReadinessDataSource()
    private val viewModel =
        BarberShopReadinessViewModel(repository)

    @Test
    fun initializationStartsReadinessObservation() {
        assertEquals(1, repository.observeCount)
        assertTrue(viewModel.uiState.isLoading)
    }

    @Test
    fun readinessSnapshotEndsLoading() {
        val readiness = BarberShopReadiness(
            profileComplete = true,
            validServiceCount = 1,
            availabilitySaved = true,
            hasOpenWorkingDay = true
        )

        repository.emit(Result.success(readiness))

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(readiness, viewModel.uiState.readiness)
        assertTrue(viewModel.uiState.readiness.isBookable)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun incompleteSnapshotIsExposedWithoutError() {
        val readiness = BarberShopReadiness(
            profileComplete = true
        )

        repository.emit(Result.success(readiness))

        assertFalse(viewModel.uiState.isLoading)
        assertFalse(viewModel.uiState.readiness.isBookable)
        assertEquals(1, viewModel.uiState.readiness.completedStepCount)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun repositoryFailureShowsReadableMessage() {
        repository.emit(
            Result.failure(
                IllegalStateException("Permission denied")
            )
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Permission denied",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun retryStopsPreviousListenerAndObservesAgain() {
        repository.emit(
            Result.failure(IllegalStateException("Failed"))
        )

        viewModel.retry()

        assertEquals(1, repository.stopCount)
        assertEquals(2, repository.observeCount)
        assertTrue(viewModel.uiState.isLoading)
        assertNull(viewModel.uiState.errorMessage)
    }
}

private class FakeReadinessDataSource :
    BarberShopReadinessDataSource {
    private var callback:
        ((Result<BarberShopReadiness>) -> Unit)? = null

    var observeCount = 0
        private set
    var stopCount = 0
        private set

    override fun observeReadiness(
        onResult: (Result<BarberShopReadiness>) -> Unit
    ): AppointmentObservation {
        observeCount += 1
        callback = onResult
        return AppointmentObservation {
            stopCount += 1
        }
    }

    fun emit(result: Result<BarberShopReadiness>) {
        callback?.invoke(result)
    }
}
