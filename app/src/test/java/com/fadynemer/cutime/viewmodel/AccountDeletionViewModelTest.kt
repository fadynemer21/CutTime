package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.AccountDeletionRequest
import com.fadynemer.cutime.model.AccountDeletionStatus
import com.fadynemer.cutime.repository.AccountDeletionDataSource
import com.fadynemer.cutime.repository.AppointmentObservation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountDeletionViewModelTest {

    @Test
    fun initialObservation_exposesExistingRequest() {
        val repository = FakeDeletionRepository()
        val viewModel = AccountDeletionViewModel(repository)
        val request = pendingRequest()

        repository.emit(Result.success(request))

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(request, viewModel.uiState.request)
    }

    @Test
    fun confirmationCanBeShownAndDismissed() {
        val repository = FakeDeletionRepository()
        val viewModel = AccountDeletionViewModel(repository)
        repository.emit(Result.success(null))

        viewModel.showConfirmation()
        assertTrue(viewModel.uiState.showConfirmation)

        viewModel.dismissConfirmation()
        assertFalse(viewModel.uiState.showConfirmation)
    }

    @Test
    fun submitIsSerializedAndUsesCurrentRole() {
        val repository = FakeDeletionRepository()
        val viewModel = AccountDeletionViewModel(repository)
        repository.emit(Result.success(null))

        viewModel.submit("BARBER")
        viewModel.submit("BARBER")

        assertTrue(viewModel.uiState.isSubmitting)
        assertEquals(1, repository.submitCalls)
        assertEquals("BARBER", repository.requestedRole)
    }

    @Test
    fun successfulSubmitClosesDialogAndShowsConfirmation() {
        val repository = FakeDeletionRepository()
        val viewModel = AccountDeletionViewModel(repository)
        repository.emit(Result.success(null))
        viewModel.showConfirmation()
        viewModel.submit("CUSTOMER")

        repository.completeSubmit(Result.success(Unit))

        assertFalse(viewModel.uiState.isSubmitting)
        assertFalse(viewModel.uiState.showConfirmation)
        assertEquals(
            "Account deletion request submitted.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun existingRequestPreventsAnotherSubmission() {
        val repository = FakeDeletionRepository()
        val viewModel = AccountDeletionViewModel(repository)
        repository.emit(Result.success(pendingRequest()))

        viewModel.showConfirmation()
        viewModel.submit("CUSTOMER")

        assertFalse(viewModel.uiState.showConfirmation)
        assertEquals(0, repository.submitCalls)
        assertNull(viewModel.uiState.successMessage)
    }

    private fun pendingRequest() = AccountDeletionRequest(
        userId = "customer",
        email = "customer@example.com",
        role = "CUSTOMER",
        status = AccountDeletionStatus.PENDING,
        requestedAtMillis = 1L
    )

    private class FakeDeletionRepository : AccountDeletionDataSource {
        private var observer:
            ((Result<AccountDeletionRequest?>) -> Unit)? = null
        private var submitCallback: ((Result<Unit>) -> Unit)? = null

        var submitCalls = 0
            private set
        var requestedRole: String? = null
            private set

        override fun observeRequest(
            onResult: (Result<AccountDeletionRequest?>) -> Unit
        ): AppointmentObservation {
            observer = onResult
            return AppointmentObservation {}
        }

        override fun submitRequest(
            role: String,
            onResult: (Result<Unit>) -> Unit
        ) {
            submitCalls += 1
            requestedRole = role
            submitCallback = onResult
        }

        fun emit(result: Result<AccountDeletionRequest?>) {
            observer?.invoke(result)
        }

        fun completeSubmit(result: Result<Unit>) {
            submitCallback?.invoke(result)
        }
    }
}
