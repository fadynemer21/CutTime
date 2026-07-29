package com.fadynemer.cutime.viewmodel

import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.repository.AuthRepository

class SessionViewModel : ViewModel() {

    private val authRepository = AuthRepository()

    fun logout() {
        authRepository.logout()
    }
}