package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.Rating
import com.fadynemer.cutime.model.RatingRequest
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.RatingDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberReviewsViewModelTest {
    @Test
    fun blankBarberId_isRejectedWithoutListener() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)

        viewModel.observe(" ")

        assertEquals(0, repository.observeCalls)
        assertEquals(
            "The barber profile is invalid.",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun observe_startsLoadingForBarber() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)

        viewModel.observe("barber_1")

        assertTrue(viewModel.uiState.isLoading)
        assertEquals("barber_1", viewModel.uiState.barberId)
        assertEquals("barber_1", repository.observedBarberId)
    }

    @Test
    fun successfulEmission_exposesRatings() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)
        val ratings = listOf(
            rating("one", "Excellent"),
            rating("two", "")
        )
        viewModel.observe("barber_1")

        repository.emit(Result.success(ratings))

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(ratings, viewModel.uiState.ratings)
    }

    @Test
    fun failedEmission_exposesRepositoryMessage() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)
        viewModel.observe("barber_1")

        repository.emit(
            Result.failure(Exception("Index unavailable"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Index unavailable",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun sameBarberDoesNotRestartActiveListener() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)

        viewModel.observe("barber_1")
        viewModel.observe("barber_1")

        assertEquals(1, repository.observeCalls)
        assertEquals(0, repository.stopCalls)
    }

    @Test
    fun differentBarberStopsPreviousListener() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)

        viewModel.observe("barber_1")
        viewModel.observe("barber_2")

        assertEquals(2, repository.observeCalls)
        assertEquals(1, repository.stopCalls)
        assertEquals("barber_2", repository.observedBarberId)
    }

    @Test
    fun retryStopsOldListenerAndStartsNewOne() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)
        viewModel.observe("barber_1")
        repository.emit(
            Result.failure(Exception("Temporary failure"))
        )

        viewModel.retry()

        assertEquals(2, repository.observeCalls)
        assertEquals(1, repository.stopCalls)
        assertTrue(viewModel.uiState.isLoading)
    }

    @Test
    fun deleteReview_deletesSelectedRating() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)

        viewModel.deleteReview("appointment_1")

        assertEquals("appointment_1", repository.deletedAppointmentId)
        assertEquals("appointment_1", viewModel.uiState.deletingRatingId)

        repository.completeDelete(Result.success(Unit))

        assertEquals(null, viewModel.uiState.deletingRatingId)
        assertEquals(null, viewModel.uiState.actionErrorMessage)
    }

    @Test
    fun failedDelete_exposesRepositoryMessage() {
        val repository = FakeRatingDataSource()
        val viewModel = BarberReviewsViewModel(repository)

        viewModel.deleteReview("appointment_1")
        repository.completeDelete(Result.failure(Exception("Delete failed")))

        assertEquals(null, viewModel.uiState.deletingRatingId)
        assertEquals("Delete failed", viewModel.uiState.actionErrorMessage)
    }

    private fun rating(
        id: String,
        review: String
    ) = Rating(
        id = id,
        appointmentId = id,
        customerId = "customer_$id",
        barberId = "barber_1",
        customerName = "Customer $id",
        stars = 5,
        review = review,
        createdAtMillis = 1_700_000_000_000L
    )

    private class FakeRatingDataSource : RatingDataSource {
        private var callback:
            ((Result<List<Rating>>) -> Unit)? = null
        private var deleteCallback:
            ((Result<Unit>) -> Unit)? = null

        var observedBarberId: String? = null
            private set
        var observeCalls = 0
            private set
        var stopCalls = 0
            private set
        var deletedAppointmentId: String? = null
            private set

        override fun submitRating(
            request: RatingRequest,
            onResult: (Result<Unit>) -> Unit
        ) {
            error("Not used")
        }

        override fun deleteRating(
            appointmentId: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            deletedAppointmentId = appointmentId
            deleteCallback = onResult
        }

        override fun observeBarberRatings(
            barberId: String,
            onResult: (Result<List<Rating>>) -> Unit
        ): AppointmentObservation {
            observedBarberId = barberId
            observeCalls += 1
            callback = onResult
            return AppointmentObservation {
                stopCalls += 1
            }
        }

        override fun observeAppointmentRating(
            appointmentId: String,
            onResult: (Result<Rating?>) -> Unit
        ): AppointmentObservation {
            error("Not used")
        }

        fun emit(result: Result<List<Rating>>) {
            callback?.invoke(result)
        }

        fun completeDelete(result: Result<Unit>) {
            deleteCallback?.invoke(result)
        }
    }
}
