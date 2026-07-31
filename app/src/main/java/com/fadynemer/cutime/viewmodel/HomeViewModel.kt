package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.model.CatalogSource
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberCatalogDataSource
import com.fadynemer.cutime.repository.BarberCatalogRepository

data class HomeUiState(
    val isLoading: Boolean = true,
    val barbers: List<BarberShop> = emptyList(),
    val source: CatalogSource = CatalogSource.FIRESTORE,
    val errorMessage: String? = null
)

class HomeViewModel(
    private val repository:
        BarberCatalogDataSource = BarberCatalogRepository()
) : ViewModel() {

    var uiState by mutableStateOf(HomeUiState())
        private set

    private var observation: AppointmentObservation? = null

    init {
        observeCatalog()
    }

    fun retry() {
        observation?.stop()
        observation = null
        uiState = HomeUiState()
        observeCatalog()
    }

    private fun observeCatalog() {
        observation =
            repository.observeCatalog { result ->
                result
                    .onSuccess { catalog ->
                        uiState = HomeUiState(
                            isLoading = false,
                            barbers = catalog.barbers,
                            source = catalog.source
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isLoading = false,
                            errorMessage =
                                error.localizedMessage
                                    ?: "Barbers could not be loaded."
                        )
                    }
            }
    }

    override fun onCleared() {
        observation?.stop()
        observation = null
        super.onCleared()
    }
}
