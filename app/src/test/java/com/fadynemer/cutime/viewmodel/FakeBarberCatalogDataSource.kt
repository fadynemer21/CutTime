package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.BarberCatalog
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberCatalogDataSource

internal class FakeBarberCatalogDataSource :
    BarberCatalogDataSource {
    private var catalogCallback:
        ((Result<BarberCatalog>) -> Unit)? = null
    private var occupiedCallback:
        ((Result<Set<String>>) -> Unit)? = null

    var catalogObserveCalls = 0
        private set
    var catalogStopCalls = 0
        private set
    var occupiedObserveCalls = 0
        private set
    var occupiedStopCalls = 0
        private set
    var observedBarberId: String? = null
        private set
    var observedDate: String? = null
        private set

    override fun observeCatalog(
        onResult: (Result<BarberCatalog>) -> Unit
    ): AppointmentObservation {
        catalogObserveCalls += 1
        catalogCallback = onResult
        return AppointmentObservation {
            catalogStopCalls += 1
        }
    }

    override fun loadBarber(
        barberId: String,
        onResult: (Result<BarberShop?>) -> Unit
    ) {
        onResult(Result.success(null))
    }

    override fun observeOccupiedTimes(
        barberId: String,
        date: String,
        onResult: (Result<Set<String>>) -> Unit
    ): AppointmentObservation {
        occupiedObserveCalls += 1
        observedBarberId = barberId
        observedDate = date
        occupiedCallback = onResult
        return AppointmentObservation {
            occupiedStopCalls += 1
        }
    }

    fun emitCatalog(result: Result<BarberCatalog>) {
        catalogCallback?.invoke(result)
    }

    fun emitOccupied(result: Result<Set<String>>) {
        occupiedCallback?.invoke(result)
    }
}
