package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.UserProfile
import com.fadynemer.cutime.repository.AuthRepository
import com.fadynemer.cutime.repository.ProfileDataSource

data class CustomerProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val errorMessage: String? = null,
    val isEditing: Boolean = false,
    val editedName: String = "",
    val editError: String? = null,
    val isSaving: Boolean = false,
    val saveMessage: String? = null
)

class CustomerProfileViewModel(
    private val repository: ProfileDataSource = AuthRepository()
) : ViewModel() {

    var uiState by mutableStateOf(CustomerProfileUiState())
        private set

    init {
        load()
    }

    fun load() {
        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null
        )
        repository.getCurrentUserProfile { result ->
            result
                .onSuccess { profile ->
                    uiState = CustomerProfileUiState(
                        isLoading = false,
                        profile = profile
                    )
                }
                .onFailure { error ->
                    uiState = CustomerProfileUiState(
                        isLoading = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Profile could not be loaded."
                    )
                }
        }
    }

    fun beginEditing() {
        val profile = uiState.profile ?: return
        uiState = uiState.copy(
            isEditing = true,
            editedName = profile.fullName,
            editError = null,
            saveMessage = null
        )
    }

    fun updateEditedName(value: String) {
        if (value.length <= 60) {
            uiState = uiState.copy(
                editedName = value,
                editError = null
            )
        }
    }

    fun cancelEditing() {
        uiState = uiState.copy(
            isEditing = false,
            editedName = "",
            editError = null,
            isSaving = false
        )
    }

    fun saveName() {
        if (uiState.isSaving) {
            return
        }

        val cleanName = uiState.editedName.trim()
        val validationError = validateName(cleanName)

        if (validationError != null) {
            uiState = uiState.copy(editError = validationError)
            return
        }

        if (cleanName == uiState.profile?.fullName) {
            uiState = uiState.copy(
                isEditing = false,
                editedName = "",
                editError = null
            )
            return
        }

        uiState = uiState.copy(
            isSaving = true,
            editError = null,
            saveMessage = null
        )
        repository.updateFullName(cleanName) { result ->
            result
                .onSuccess { profile ->
                    uiState = uiState.copy(
                        profile = profile,
                        isEditing = false,
                        editedName = "",
                        isSaving = false,
                        editError = null,
                        saveMessage = "Profile updated."
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        editError =
                            error.localizedMessage
                                ?: "Profile could not be updated."
                    )
                }
        }
    }

    fun dismissSaveMessage() {
        uiState = uiState.copy(saveMessage = null)
    }

    companion object {
        fun validateName(name: String): String? {
            return when {
                name.isBlank() -> "Enter your full name."
                name.length < 2 ->
                    "Name must contain at least 2 characters."
                name.length > 60 ->
                    "Name must contain no more than 60 characters."
                name.any { it == '\n' || it == '\r' } ->
                    "Name must stay on one line."
                else -> null
            }
        }
    }
}
