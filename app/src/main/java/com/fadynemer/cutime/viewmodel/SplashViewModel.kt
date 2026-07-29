package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.repository.AuthRepository

data class SplashUiState(
    val isCheckingSession: Boolean = true,
    val destination: String? = null
)

class SplashViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    var uiState by mutableStateOf(SplashUiState())
        private set

    init {
        checkCurrentSession()
    }

    private fun checkCurrentSession() {
        authRepository.getCurrentUserProfile { result ->

            result
                .onSuccess { userProfile ->

                    val destination =
                        when (userProfile?.role) {
                            "CUSTOMER" -> "home"
                            "BARBER" -> "dashboard"
                            else -> "welcome"
                        }

                    uiState = SplashUiState(
                        isCheckingSession = false,
                        destination = destination
                    )
                }
                .onFailure {
                    /*
                     * If the profile cannot be checked, send the user to
                     * Welcome instead of leaving Splash stuck forever.
                     */
                    uiState = SplashUiState(
                        isCheckingSession = false,
                        destination = "welcome"
                    )
                }
        }
    }
}