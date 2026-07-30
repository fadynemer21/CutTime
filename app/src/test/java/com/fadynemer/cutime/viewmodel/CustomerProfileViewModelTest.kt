package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.UserProfile
import com.fadynemer.cutime.repository.ProfileDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomerProfileViewModelTest {
    @Test
    fun initialLoad_exposesCurrentProfile() {
        val repository = FakeProfileDataSource()
        val viewModel = CustomerProfileViewModel(repository)
        val profile = customer(fullName = "Fady Nemer")

        repository.completeLoad(Result.success(profile))

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(profile, viewModel.uiState.profile)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun initialLoad_failureShowsRepositoryMessage() {
        val repository = FakeProfileDataSource()
        val viewModel = CustomerProfileViewModel(repository)

        repository.completeLoad(
            Result.failure(Exception("Firestore unavailable"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Firestore unavailable",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun beginEditing_prefillsCurrentName() {
        val repository = FakeProfileDataSource()
        val viewModel = CustomerProfileViewModel(repository)
        repository.completeLoad(
            Result.success(customer(fullName = "Fady Nemer"))
        )

        viewModel.beginEditing()

        assertTrue(viewModel.uiState.isEditing)
        assertEquals("Fady Nemer", viewModel.uiState.editedName)
    }

    @Test
    fun beginEditing_withoutProfileDoesNothing() {
        val repository = FakeProfileDataSource()
        val viewModel = CustomerProfileViewModel(repository)
        repository.completeLoad(Result.success(null))

        viewModel.beginEditing()

        assertFalse(viewModel.uiState.isEditing)
    }

    @Test
    fun updateEditedName_capsInputAtSixtyCharacters() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        val tooLong = "a".repeat(61)

        viewModel.updateEditedName(tooLong)

        assertEquals("Fady Nemer", viewModel.uiState.editedName)
    }

    @Test
    fun blankName_isRejectedWithoutRepositoryCall() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        viewModel.updateEditedName(" ")

        viewModel.saveName()

        assertEquals(
            "Enter your full name.",
            viewModel.uiState.editError
        )
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun oneCharacterName_isRejected() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        viewModel.updateEditedName("F")

        viewModel.saveName()

        assertEquals(
            "Name must contain at least 2 characters.",
            viewModel.uiState.editError
        )
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun unchangedName_closesEditorWithoutWrite() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()

        viewModel.saveName()

        assertFalse(viewModel.uiState.isEditing)
        assertEquals(0, repository.updateCalls)
    }

    @Test
    fun save_trimsNameBeforeRepositoryWrite() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        viewModel.updateEditedName("  Fady N.  ")

        viewModel.saveName()

        assertTrue(viewModel.uiState.isSaving)
        assertEquals("Fady N.", repository.requestedName)
    }

    @Test
    fun successfulSave_updatesDisplayedProfileAndMessage() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        viewModel.updateEditedName("Fady N.")
        viewModel.saveName()

        repository.completeUpdate(
            Result.success(customer(fullName = "Fady N."))
        )

        assertFalse(viewModel.uiState.isSaving)
        assertFalse(viewModel.uiState.isEditing)
        assertEquals(
            "Fady N.",
            viewModel.uiState.profile?.fullName
        )
        assertEquals(
            "Profile updated.",
            viewModel.uiState.saveMessage
        )
    }

    @Test
    fun failedSave_keepsEditorOpenForCorrectionOrRetry() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        viewModel.updateEditedName("New Name")
        viewModel.saveName()

        repository.completeUpdate(
            Result.failure(Exception("Permission denied"))
        )

        assertFalse(viewModel.uiState.isSaving)
        assertTrue(viewModel.uiState.isEditing)
        assertEquals("New Name", viewModel.uiState.editedName)
        assertEquals(
            "Permission denied",
            viewModel.uiState.editError
        )
    }

    @Test
    fun secondSaveWhilePending_isIgnored() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        viewModel.updateEditedName("New Name")

        viewModel.saveName()
        viewModel.saveName()

        assertEquals(1, repository.updateCalls)
    }

    @Test
    fun cancelEditing_discardsDraftAndErrors() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        viewModel.updateEditedName(" ")
        viewModel.saveName()

        viewModel.cancelEditing()

        assertFalse(viewModel.uiState.isEditing)
        assertEquals("", viewModel.uiState.editedName)
        assertNull(viewModel.uiState.editError)
    }

    @Test
    fun dismissSaveMessage_clearsConfirmation() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)
        viewModel.beginEditing()
        viewModel.updateEditedName("New Name")
        viewModel.saveName()
        repository.completeUpdate(
            Result.success(customer(fullName = "New Name"))
        )

        viewModel.dismissSaveMessage()

        assertNull(viewModel.uiState.saveMessage)
    }

    @Test
    fun reload_reentersLoadingAndRequestsAgain() {
        val repository = FakeProfileDataSource()
        val viewModel = loadedViewModel(repository)

        viewModel.load()

        assertTrue(viewModel.uiState.isLoading)
        assertEquals(2, repository.loadCalls)
    }

    @Test
    fun validateName_rejectsLineBreaks() {
        assertEquals(
            "Name must stay on one line.",
            CustomerProfileViewModel.validateName("Fady\nNemer")
        )
    }

    @Test
    fun validateName_acceptsNormalTwoCharacterName() {
        assertNull(CustomerProfileViewModel.validateName("Fa"))
    }

    private fun loadedViewModel(
        repository: FakeProfileDataSource
    ): CustomerProfileViewModel {
        return CustomerProfileViewModel(repository).also {
            repository.completeLoad(
                Result.success(customer(fullName = "Fady Nemer"))
            )
        }
    }

    private fun customer(
        fullName: String
    ) = UserProfile(
        uid = "customer_1",
        fullName = fullName,
        email = "customer@example.com",
        role = "CUSTOMER"
    )

    private class FakeProfileDataSource : ProfileDataSource {
        private var loadCallback:
            ((Result<UserProfile?>) -> Unit)? = null
        private var updateCallback:
            ((Result<UserProfile>) -> Unit)? = null

        var loadCalls = 0
            private set
        var updateCalls = 0
            private set
        var requestedName: String? = null
            private set

        override fun getCurrentUserProfile(
            onResult: (Result<UserProfile?>) -> Unit
        ) {
            loadCalls += 1
            loadCallback = onResult
        }

        override fun updateFullName(
            fullName: String,
            onResult: (Result<UserProfile>) -> Unit
        ) {
            updateCalls += 1
            requestedName = fullName
            updateCallback = onResult
        }

        fun completeLoad(result: Result<UserProfile?>) {
            loadCallback?.invoke(result)
        }

        fun completeUpdate(result: Result<UserProfile>) {
            updateCallback?.invoke(result)
        }
    }
}
