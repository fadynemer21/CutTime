package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.model.RescheduleRequest
import com.fadynemer.cutime.repository.AppointmentActionsDataSource
import com.fadynemer.cutime.repository.AppointmentObservation

internal fun testAppointment(
    id: String = "appointment_1",
    customerId: String = "customer_1",
    customerName: String = "Fady Customer",
    barberId: String = "barber_1",
    barberName: String = "Urban Fade Studio",
    status: AppointmentStatus = AppointmentStatus.UPCOMING,
    startAtMillis: Long = 4_102_444_200_000L,
    endAtMillis: Long = 4_102_446_000_000L,
    ratingId: String? = null
) = Appointment(
    id = id,
    customerId = customerId,
    customerName = customerName,
    customerEmail = "customer@example.com",
    barberId = barberId,
    barberName = barberName,
    serviceId = "service_1",
    serviceName = "Classic Haircut",
    price = 60,
    durationMinutes = 30,
    appointmentDate = "2099-12-31",
    appointmentTime = "14:30",
    startAtMillis = startAtMillis,
    endAtMillis = endAtMillis,
    status = status,
    ratingId = ratingId,
    createdAtMillis = 1_700_000_000_000L,
    updatedAtMillis = 1_700_000_100_000L
)

internal class FakeAppointmentActionsDataSource :
    AppointmentActionsDataSource {
    private var observationCallback:
        ((Result<Appointment?>) -> Unit)? = null
    private var cancelCallback:
        ((Result<Unit>) -> Unit)? = null
    private var completeCallback:
        ((Result<Unit>) -> Unit)? = null
    private var rescheduleCallback:
        ((Result<Unit>) -> Unit)? = null

    var observedAppointmentId: String? = null
        private set
    var cancelledAppointmentId: String? = null
        private set
    var completedAppointmentId: String? = null
        private set
    var hiddenAppointmentId: String? = null
        private set
    var rescheduleRequest: RescheduleRequest? = null
        private set
    var observationStopCount = 0
        private set
    var cancelCalls = 0
        private set
    var completeCalls = 0
        private set
    var rescheduleCalls = 0
        private set

    override fun observeAppointment(
        appointmentId: String,
        onResult: (Result<Appointment?>) -> Unit
    ): AppointmentObservation {
        observedAppointmentId = appointmentId
        observationCallback = onResult
        return AppointmentObservation {
            observationStopCount += 1
        }
    }

    override fun cancelAppointment(
        appointmentId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        cancelCalls += 1
        cancelledAppointmentId = appointmentId
        cancelCallback = onResult
    }

    override fun completeAppointment(
        appointmentId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        completeCalls += 1
        completedAppointmentId = appointmentId
        completeCallback = onResult
    }

    override fun hideCancelledAppointment(
        appointmentId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        hiddenAppointmentId = appointmentId
        onResult(Result.success(Unit))
    }

    override fun rescheduleAppointment(
        request: RescheduleRequest,
        onResult: (Result<Unit>) -> Unit
    ) {
        rescheduleCalls += 1
        rescheduleRequest = request
        rescheduleCallback = onResult
    }

    fun emitAppointment(result: Result<Appointment?>) {
        observationCallback?.invoke(result)
    }

    fun completeCancel(result: Result<Unit>) {
        cancelCallback?.invoke(result)
    }

    fun completeComplete(result: Result<Unit>) {
        completeCallback?.invoke(result)
    }

    fun completeReschedule(result: Result<Unit>) {
        rescheduleCallback?.invoke(result)
    }
}
