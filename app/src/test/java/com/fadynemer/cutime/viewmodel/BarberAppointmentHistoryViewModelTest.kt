package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.BarberAppointmentDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberAppointmentHistoryViewModelTest {
    @Test
    fun emission_keepsOnlyHistoryAndSortsAllRecordsNewestFirst() {
        val repository = FakeBarberHistoryDataSource()
        val viewModel = BarberAppointmentHistoryViewModel(repository)

        repository.emit(
            Result.success(
                listOf(
                    appointment("old-complete", AppointmentStatus.COMPLETED, 10),
                    appointment("upcoming", AppointmentStatus.UPCOMING, 40),
                    appointment("cancelled", AppointmentStatus.CANCELLED, 30),
                    appointment("new-complete", AppointmentStatus.COMPLETED, 20)
                )
            )
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            listOf("cancelled", "new-complete", "old-complete"),
            viewModel.uiState.appointments.map(Appointment::id)
        )
    }

    @Test
    fun emptyEmission_isEmpty() {
        val repository = FakeBarberHistoryDataSource()
        val viewModel = BarberAppointmentHistoryViewModel(repository)

        repository.emit(Result.success(emptyList()))

        assertTrue(viewModel.uiState.isEmpty)
    }

    @Test
    fun retryStopsOldListenerAndStartsAnother() {
        val repository = FakeBarberHistoryDataSource()
        val viewModel = BarberAppointmentHistoryViewModel(repository)

        viewModel.retry()

        assertEquals(2, repository.observeCalls)
        assertEquals(1, repository.stopCalls)
    }
}

private class FakeBarberHistoryDataSource : BarberAppointmentDataSource {
    var observeCalls = 0
    var stopCalls = 0
    private var callback: ((Result<List<Appointment>>) -> Unit)? = null

    override fun observeBarberAppointments(
        onResult: (Result<List<Appointment>>) -> Unit
    ): AppointmentObservation {
        observeCalls++
        callback = onResult
        return AppointmentObservation { stopCalls++ }
    }

    fun emit(result: Result<List<Appointment>>) = callback?.invoke(result)
}

private fun appointment(
    id: String,
    status: AppointmentStatus,
    startAtMillis: Long
) = Appointment(
    id = id,
    customerId = "customer",
    barberId = "barber",
    barberName = "Barber",
    serviceId = "service",
    serviceName = "Cut",
    price = 50,
    durationMinutes = 30,
    appointmentDate = "2026-08-05",
    appointmentTime = "10:00",
    startAtMillis = startAtMillis,
    endAtMillis = startAtMillis + 30,
    status = status
)
