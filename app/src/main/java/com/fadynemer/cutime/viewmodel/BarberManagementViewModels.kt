package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.DayAvailability
import com.fadynemer.cutime.model.ManagedBarberProfile
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberDataSource
import com.fadynemer.cutime.repository.BarberRepository
import com.fadynemer.cutime.util.BarberManagementValidator

data class BarberProfileUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val shopName: String = "",
    val description: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BarberProfileViewModel(
    private val repository:
        BarberDataSource = BarberRepository()
) : ViewModel() {

    var uiState by mutableStateOf(BarberProfileUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observation =
            repository.observeProfile { result ->
                result
                    .onSuccess { profile ->
                        uiState = uiState.copy(
                            isLoading = false,
                            shopName = profile?.shopName.orEmpty(),
                            description = profile?.description.orEmpty(),
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = readableError(error)
                        )
                    }
            }
    }

    fun updateShopName(value: String) {
        uiState = uiState.copy(
            shopName = value,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateDescription(value: String) {
        uiState = uiState.copy(
            description = value.take(500),
            errorMessage = null,
            successMessage = null
        )
    }

    fun save() {
        if (uiState.isSaving) return

        val profile = ManagedBarberProfile(
            shopName = uiState.shopName.trim(),
            description = uiState.description.trim()
        )
        val validationError =
            BarberManagementValidator.validateProfile(profile)

        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError)
            return
        }

        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null
        )
        repository.saveProfile(profile) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isSaving = false,
                        successMessage = "Profile saved."
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        errorMessage = readableError(error)
                    )
                }
        }
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}

data class BarberServicesUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val services: List<BarberService> = emptyList(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BarberServicesViewModel(
    private val repository:
        BarberDataSource = BarberRepository()
) : ViewModel() {

    var uiState by mutableStateOf(BarberServicesUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observation =
            repository.observeServices { result ->
                result
                    .onSuccess { services ->
                        uiState = uiState.copy(
                            isLoading = false,
                            services = services,
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = readableError(error)
                        )
                    }
            }
    }

    fun saveService(
        service: BarberService,
        onSaved: () -> Unit
    ) {
        if (uiState.isSaving) return

        val validationError =
            BarberManagementValidator.validateService(service)

        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError)
            return
        }

        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null
        )
        repository.saveService(service) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isSaving = false,
                        successMessage = "Service saved."
                    )
                    onSaved()
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        errorMessage = readableError(error)
                    )
                }
        }
    }

    fun deleteService(
        serviceId: String
    ) {
        if (uiState.isSaving) return

        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null
        )
        repository.deleteService(serviceId) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isSaving = false,
                        successMessage = "Service removed."
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        errorMessage = readableError(error)
                    )
                }
        }
    }

    fun clearMessage() {
        uiState = uiState.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}

data class BarberAvailabilityUiState(
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val availability: BarberAvailability = BarberAvailability(),
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class BarberAvailabilityViewModel(
    private val repository:
        BarberDataSource = BarberRepository()
) : ViewModel() {

    var uiState by mutableStateOf(BarberAvailabilityUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observation =
            repository.observeAvailability { result ->
                result
                    .onSuccess { availability ->
                        uiState = uiState.copy(
                            isLoading = false,
                            availability = availability,
                            errorMessage = null
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage = readableError(error)
                        )
                    }
            }
    }

    fun updateDay(
        dayAvailability: DayAvailability
    ) {
        uiState = uiState.copy(
            availability =
                uiState.availability.copy(
                    days =
                        uiState.availability.days.map { day ->
                            if (day.day == dayAvailability.day) {
                                dayAvailability
                            } else {
                                day
                            }
                        }
                ),
            errorMessage = null,
            successMessage = null
        )
    }

    fun addBlockedDate(value: String) {
        val updated =
            uiState.availability.copy(
                blockedDates =
                    (
                        uiState.availability.blockedDates +
                            value.trim()
                        )
                        .filter(String::isNotBlank)
                        .distinct()
            )
        val validationError =
            BarberManagementValidator.validateAvailability(updated)

        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError)
        } else {
            uiState = uiState.copy(
                availability = updated,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun removeBlockedDate(value: String) {
        uiState = uiState.copy(
            availability =
                uiState.availability.copy(
                    blockedDates =
                        uiState.availability.blockedDates - value
                ),
            successMessage = null
        )
    }

    fun save() {
        if (uiState.isSaving) return

        val validationError =
            BarberManagementValidator.validateAvailability(
                uiState.availability
            )

        if (validationError != null) {
            uiState = uiState.copy(errorMessage = validationError)
            return
        }

        uiState = uiState.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null
        )
        repository.saveAvailability(
            uiState.availability
        ) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        isSaving = false,
                        successMessage = "Availability saved."
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSaving = false,
                        errorMessage = readableError(error)
                    )
                }
        }
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}

private fun readableError(
    error: Throwable
): String {
    return error.localizedMessage
        ?: "The request could not be completed."
}
