package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.repository.AuthRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException

data class LoginUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val loginSuccessful: Boolean = false,
    val loggedInRole: String? = null
)

class LoginViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    var uiState by mutableStateOf(LoginUiState())
        private set

    fun loginUser(
        email: String,
        password: String
    ) {
        if (uiState.isLoading) {
            return
        }

        uiState = LoginUiState(
            isLoading = true
        )

        authRepository.loginUser(
            email = email,
            password = password
        ) { result ->

            result
                .onSuccess { userProfile ->
                    uiState = LoginUiState(
                        isLoading = false,
                        loginSuccessful = true,
                        loggedInRole = userProfile.role
                    )
                }
                .onFailure { error ->
                    uiState = LoginUiState(
                        isLoading = false,
                        errorMessage = createReadableLoginError(error)
                    )
                }
        }
    }

    fun sendPasswordResetEmail(
        email: String
    ) {
        if (uiState.isLoading) {
            return
        }

        uiState = uiState.copy(
            isLoading = true,
            errorMessage = null,
            successMessage = null
        )

        authRepository.sendPasswordResetEmail(
            email = email
        ) { result ->

            result
                .onSuccess {
                    uiState = uiState.copy(
                        isLoading = false,
                        successMessage =
                            " \"If an account exists for this email," +
                                    " a password reset link will be sent.\""
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage =
                            createReadableResetError(error)
                    )
                }
        }
    }

    fun clearError() {
        uiState = uiState.copy(
            errorMessage = null
        )
    }

    fun clearMessages() {
        uiState = uiState.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    private fun createReadableLoginError(
        error: Throwable
    ): String {
        return when (error) {
            is FirebaseAuthInvalidCredentialsException ->
                "Incorrect email or password."

            is FirebaseAuthInvalidUserException ->
                "Incorrect email or password."

            is FirebaseNetworkException ->
                "Check your internet connection and try again."

            is FirebaseTooManyRequestsException ->
                "Too many attempts. Please wait and try again."

            else ->
                error.localizedMessage
                    ?: "Login failed. Please try again."
        }
    }

    private fun createReadableResetError(
        error: Throwable
    ): String {
        return when (error) {
            is FirebaseAuthInvalidCredentialsException ->
                "Enter a valid email address."

            is FirebaseNetworkException ->
                "Check your internet connection and try again."

            is FirebaseTooManyRequestsException ->
                "Too many attempts. Please wait and try again."

            else ->
                error.localizedMessage
                    ?: "Password reset failed. Please try again."
        }
    }
}