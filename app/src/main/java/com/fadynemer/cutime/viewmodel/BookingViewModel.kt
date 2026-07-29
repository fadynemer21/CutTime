package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

data class BookingUiState(
    val selectedServiceId: String? = null,
    val selectedDate: String? = null,
    val selectedTime: String? = null,
    val isReviewing: Boolean = false
) {
    val canReview: Boolean
        get() =
            selectedServiceId != null &&
                selectedDate != null &&
                selectedTime != null
}

class BookingViewModel : ViewModel() {

    var uiState by mutableStateOf(BookingUiState())
        private set

    fun selectService(serviceId: String) {
        uiState = uiState.copy(
            selectedServiceId = serviceId,
            isReviewing = false
        )
    }

    fun selectDate(date: String) {
        uiState = uiState.copy(
            selectedDate = date,
            selectedTime = null,
            isReviewing = false
        )
    }

    fun selectTime(time: String) {
        uiState = uiState.copy(
            selectedTime = time,
            isReviewing = false
        )
    }

    fun reviewBooking() {
        if (uiState.canReview) {
            uiState = uiState.copy(
                isReviewing = true
            )
        }
    }

    fun editBooking() {
        uiState = uiState.copy(
            isReviewing = false
        )
    }
}
