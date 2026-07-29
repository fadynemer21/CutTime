package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

data class RegisterUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val registrationSuccessful: Boolean = false,
    val registeredRole: String? = null
)

class RegisterViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    var uiState by mutableStateOf(RegisterUiState())
        private set

    fun registerUser(
        fullName: String,
        email: String,
        password: String,
        role: String
    ) {
        if (uiState.isLoading) {
            return
        }

        uiState = RegisterUiState(
            isLoading = true
        )

        authRepository.registerUser(
            fullName = fullName,
            email = email,
            password = password,
            role = role
        ) { result ->

            result
                .onSuccess { userProfile ->
                    uiState = RegisterUiState(
                        isLoading = false,
                        registrationSuccessful = true,
                        registeredRole = userProfile.role
                    )
                }
                .onFailure { error ->
                    uiState = RegisterUiState(
                        isLoading = false,
                        errorMessage = createReadableError(error)
                    )
                }
        }
    }

    fun clearError() {
        uiState = uiState.copy(
            errorMessage = null
        )
    }

    private fun createReadableError(error: Throwable): String {
        return when (error) {
            is FirebaseAuthUserCollisionException ->
                "An account already exists with this email."

            is FirebaseAuthWeakPasswordException ->
                "This password does not meet the security requirements."

            is FirebaseAuthInvalidCredentialsException ->
                "Please enter a valid email address."

            is FirebaseNetworkException ->
                "Check your internet connection and try again."

            else ->
                error.localizedMessage
                    ?: "Account creation failed. Please try again."
        }
    }
}