package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.data.SampleBarberData
import com.fadynemer.cutime.model.BarberCatalog
import com.fadynemer.cutime.model.CatalogSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeViewModelTest {
    @Test
    fun initialization_startsCatalogListener() {
        val repository = FakeBarberCatalogDataSource()

        val viewModel = HomeViewModel(repository)

        assertEquals(1, repository.catalogObserveCalls)
        assertTrue(viewModel.uiState.isLoading)
    }

    @Test
    fun firestoreCatalog_populatesHome() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = HomeViewModel(repository)
        val barbers = SampleBarberData.barberShops.take(2)

        repository.emitCatalog(
            Result.success(
                BarberCatalog(
                    barbers = barbers,
                    source = CatalogSource.FIRESTORE
                )
            )
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(barbers, viewModel.uiState.barbers)
        assertEquals(
            CatalogSource.FIRESTORE,
            viewModel.uiState.source
        )
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun developmentFallback_isClearlyRepresentedInState() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = HomeViewModel(repository)
        val previews = SampleBarberData.barberShops.map {
            it.copy(isDevelopmentFallback = true)
        }

        repository.emitCatalog(
            Result.success(
                BarberCatalog(
                    barbers = previews,
                    source = CatalogSource.DEVELOPMENT_FALLBACK
                )
            )
        )

        assertEquals(
            CatalogSource.DEVELOPMENT_FALLBACK,
            viewModel.uiState.source
        )
        assertTrue(
            viewModel.uiState.barbers.all {
                it.isDevelopmentFallback
            }
        )
    }

    @Test
    fun catalogFailure_exposesMessageAndStopsLoading() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = HomeViewModel(repository)

        repository.emitCatalog(
            Result.failure(Exception("Catalog listener failed"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Catalog listener failed",
            viewModel.uiState.errorMessage
        )
        assertTrue(viewModel.uiState.barbers.isEmpty())
    }

    @Test
    fun retry_stopsOldListenerAndStartsFreshLoadingState() {
        val repository = FakeBarberCatalogDataSource()
        val viewModel = HomeViewModel(repository)
        repository.emitCatalog(
            Result.failure(Exception("Temporary"))
        )

        viewModel.retry()

        assertEquals(1, repository.catalogStopCalls)
        assertEquals(2, repository.catalogObserveCalls)
        assertTrue(viewModel.uiState.isLoading)
        assertNull(viewModel.uiState.errorMessage)
    }
}
