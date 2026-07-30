package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.model.Rating
import com.fadynemer.cutime.model.RatingRequest
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.RatingDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RatingViewModelTest {
    @Test
    fun observe_startsAppointmentAndRatingListeners() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = RatingViewModel(appointments, ratings)

        viewModel.observe("appointment_1")

        assertEquals(
            "appointment_1",
            appointments.observedAppointmentId
        )
        assertEquals(
            "appointment_1",
            ratings.observedAppointmentId
        )
        assertTrue(viewModel.uiState.isLoading)
    }

    @Test
    fun appointmentEmission_populatesEligibilityContext() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = RatingViewModel(appointments, ratings)
        val appointment = testAppointment(
            status = AppointmentStatus.COMPLETED
        )
        viewModel.observe(appointment.id)

        appointments.emitAppointment(
            Result.success(appointment)
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(appointment, viewModel.uiState.appointment)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun missingAppointment_exposesMessage() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = RatingViewModel(appointments, ratings)
        viewModel.observe("missing")

        appointments.emitAppointment(Result.success(null))

        assertEquals(
            "Appointment not found.",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun selectingStars_coercesLowValueToOne() {
        val viewModel = viewModel()

        viewModel.selectStars(-4)

        assertEquals(1, viewModel.uiState.stars)
    }

    @Test
    fun selectingStars_coercesHighValueToFive() {
        val viewModel = viewModel()

        viewModel.selectStars(20)

        assertEquals(5, viewModel.uiState.stars)
    }

    @Test
    fun reviewInput_isCappedAtFiveHundredCharacters() {
        val viewModel = viewModel()
        val longReview = "x".repeat(650)

        viewModel.updateReview(longReview)

        assertEquals(500, viewModel.uiState.review.length)
    }

    @Test
    fun existingRating_isExposedByListener() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = RatingViewModel(appointments, ratings)
        viewModel.observe("appointment_1")
        val rating = rating()

        ratings.emitAppointmentRating(Result.success(rating))

        assertEquals(rating, viewModel.uiState.existingRating)
    }

    @Test
    fun submitWithoutLoadedAppointment_isIgnored() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = RatingViewModel(appointments, ratings)

        viewModel.submit()

        assertEquals(0, ratings.submitCalls)
    }

    @Test
    fun submitBuildsRatingForLoadedBarberAndAppointment() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = loadedViewModel(appointments, ratings)
        viewModel.selectStars(5)
        viewModel.updateReview("Excellent cut.")

        viewModel.submit()

        assertTrue(viewModel.uiState.isSubmitting)
        assertEquals(
            RatingRequest(
                appointmentId = "appointment_1",
                barberId = "barber_1",
                stars = 5,
                review = "Excellent cut."
            ),
            ratings.submittedRequest
        )
    }

    @Test
    fun successfulSubmit_marksFlowSuccessful() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = loadedViewModel(appointments, ratings)
        viewModel.selectStars(4)
        viewModel.submit()

        ratings.completeSubmit(Result.success(Unit))

        assertFalse(viewModel.uiState.isSubmitting)
        assertTrue(viewModel.uiState.isSuccessful)
    }

    @Test
    fun failedSubmit_exposesRepositoryMessage() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = loadedViewModel(appointments, ratings)
        viewModel.selectStars(4)
        viewModel.submit()

        ratings.completeSubmit(
            Result.failure(Exception("Already rated"))
        )

        assertFalse(viewModel.uiState.isSubmitting)
        assertEquals(
            "Already rated",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun duplicateSubmitWhilePending_isIgnored() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = loadedViewModel(appointments, ratings)
        viewModel.selectStars(5)

        viewModel.submit()
        viewModel.submit()

        assertEquals(1, ratings.submitCalls)
    }

    @Test
    fun existingRating_preventsNewSubmission() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = loadedViewModel(appointments, ratings)
        ratings.emitAppointmentRating(
            Result.success(rating())
        )
        viewModel.selectStars(5)

        viewModel.submit()

        assertEquals(0, ratings.submitCalls)
    }

    @Test
    fun observingAnotherAppointment_stopsBothOldListeners() {
        val appointments = FakeAppointmentActionsDataSource()
        val ratings = FakeRatingDataSource()
        val viewModel = RatingViewModel(appointments, ratings)

        viewModel.observe("appointment_1")
        viewModel.observe("appointment_2")

        assertEquals(1, appointments.observationStopCount)
        assertEquals(1, ratings.appointmentStopCount)
    }

    private fun viewModel(): RatingViewModel {
        return RatingViewModel(
            FakeAppointmentActionsDataSource(),
            FakeRatingDataSource()
        )
    }

    private fun loadedViewModel(
        appointments: FakeAppointmentActionsDataSource,
        ratings: FakeRatingDataSource
    ): RatingViewModel {
        return RatingViewModel(
            appointments,
            ratings
        ).also {
            it.observe("appointment_1")
            appointments.emitAppointment(
                Result.success(
                    testAppointment(
                        status = AppointmentStatus.COMPLETED
                    )
                )
            )
        }
    }

    private fun rating() = Rating(
        id = "appointment_1",
        appointmentId = "appointment_1",
        customerId = "customer_1",
        barberId = "barber_1",
        customerName = "Fady Customer",
        stars = 5,
        review = "Excellent cut.",
        createdAtMillis = 1_700_000_000_000L
    )

    private class FakeRatingDataSource : RatingDataSource {
        private var appointmentRatingCallback:
            ((Result<Rating?>) -> Unit)? = null
        private var submitCallback:
            ((Result<Unit>) -> Unit)? = null

        var observedAppointmentId: String? = null
            private set
        var submittedRequest: RatingRequest? = null
            private set
        var submitCalls = 0
            private set
        var appointmentStopCount = 0
            private set

        override fun submitRating(
            request: RatingRequest,
            onResult: (Result<Unit>) -> Unit
        ) {
            submitCalls += 1
            submittedRequest = request
            submitCallback = onResult
        }

        override fun observeBarberRatings(
            barberId: String,
            onResult: (Result<List<Rating>>) -> Unit
        ): AppointmentObservation {
            return AppointmentObservation {}
        }

        override fun observeAppointmentRating(
            appointmentId: String,
            onResult: (Result<Rating?>) -> Unit
        ): AppointmentObservation {
            observedAppointmentId = appointmentId
            appointmentRatingCallback = onResult
            return AppointmentObservation {
                appointmentStopCount += 1
            }
        }

        fun emitAppointmentRating(
            result: Result<Rating?>
        ) {
            appointmentRatingCallback?.invoke(result)
        }

        fun completeSubmit(result: Result<Unit>) {
            submitCallback?.invoke(result)
        }
    }
}
