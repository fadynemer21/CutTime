package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.model.BarberAvailabilityDocumentCodec
import com.fadynemer.cutime.model.BarberShopReadinessEvaluator
import com.fadynemer.cutime.model.BookingRequest
import com.fadynemer.cutime.model.RescheduleRequest
import com.fadynemer.cutime.util.AppointmentDateTime
import com.fadynemer.cutime.util.AppointmentHistoryPolicy
import com.fadynemer.cutime.util.BarberManagementValidator
import com.fadynemer.cutime.util.CustomerNameResolver
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Date
import java.time.LocalDate
import java.time.LocalTime

class BookingConflictException :
    Exception(
        "That appointment time was just booked. Please choose another time."
    )

class BookingUnavailableException :
    Exception(
        "The barber is unavailable on that date. Please choose another day."
    )

class AppointmentAuthenticationException :
    Exception("Please log in again to continue.")

class AppointmentHistoryDeletionException :
    Exception(
        "Only your completed or cancelled appointments can be removed from history."
    )

interface AppointmentBookingDataSource {
    fun createAppointment(
        request: BookingRequest,
        onResult: (Result<String>) -> Unit
    )
}

interface AppointmentListDataSource {
    fun observeCustomerAppointments(
        onResult: (Result<List<Appointment>>) -> Unit
    ): AppointmentObservation?
}

interface BarberAppointmentDataSource {
    fun observeBarberAppointments(
        onResult: (Result<List<Appointment>>) -> Unit
    ): AppointmentObservation?
}

interface AppointmentActionsDataSource {
    fun cancelAppointment(
        appointmentId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun completeAppointment(
        appointmentId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun hideCustomerAppointments(
        appointmentIds: List<String>,
        onResult: (Result<Unit>) -> Unit
    )

    fun hideBarberAppointments(
        appointmentIds: List<String>,
        onResult: (Result<Unit>) -> Unit
    )

    fun observeAppointment(
        appointmentId: String,
        onResult: (Result<Appointment?>) -> Unit
    ): AppointmentObservation

    fun rescheduleAppointment(
        request: RescheduleRequest,
        onResult: (Result<Unit>) -> Unit
    )
}

fun interface AppointmentObservation {
    fun stop()
}

class AppointmentRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) : AppointmentBookingDataSource,
    AppointmentListDataSource,
    BarberAppointmentDataSource,
    AppointmentActionsDataSource {

    override fun createAppointment(
        request: BookingRequest,
        onResult: (Result<String>) -> Unit
    ) {
        val validationError =
            AppointmentDateTime.validate(request)

        if (validationError != null) {
            onResult(
                Result.failure(
                    IllegalArgumentException(validationError)
                )
            )
            return
        }

        val customerId = auth.currentUser?.uid
        val customerEmail =
            auth.currentUser?.email.orEmpty()

        if (customerId == null) {
            onResult(
                Result.failure(
                    AppointmentAuthenticationException()
                )
            )
            return
        }

        val startAtMillis =
            AppointmentDateTime.toStartMillis(
                date = request.appointmentDate,
                time = request.appointmentTime
            )
        val endAtMillis =
            startAtMillis +
                request.durationMinutes * 60_000L
        val slotIds =
            AppointmentDateTime.slotDocumentIds(
                barberId = request.barberId,
                date = request.appointmentDate,
                time = request.appointmentTime,
                durationMinutes = request.durationMinutes
            )

        val appointmentReference =
            firestore.collection(APPOINTMENTS_COLLECTION).document()
        val customerProfileReference =
            firestore.collection(USERS_COLLECTION).document(customerId)
        val availabilityReference =
            firestore
                .collection(AVAILABILITY_COLLECTION)
                .document(request.barberId)
        val slotReferences = slotIds.map { slotId ->
            firestore.collection(BOOKING_SLOTS_COLLECTION).document(slotId)
        }

        firestore.runTransaction { transaction ->
            val customerProfile =
                transaction.get(customerProfileReference)
            val availability =
                transaction.get(availabilityReference)
            val existingSlots =
                slotReferences.map { slotReference ->
                    transaction.get(slotReference)
                }

            val customerName =
                CustomerNameResolver.resolve(
                    firestoreFullName =
                        customerProfile.getString("fullName"),
                    authenticationDisplayName =
                        auth.currentUser?.displayName
                )
            requireBookableSchedule(
                availability = availability,
                date = request.appointmentDate,
                time = request.appointmentTime,
                durationMinutes = request.durationMinutes
            )

            if (existingSlots.any(DocumentSnapshot::exists)) {
                throw BookingConflictException()
            }

            val appointmentData = hashMapOf(
                "appointmentId" to appointmentReference.id,
                "customerId" to customerId,
                "customerName" to customerName,
                "customerEmail" to customerEmail,
                "barberId" to request.barberId,
                "barberName" to request.barberName,
                "serviceId" to request.serviceId,
                "serviceName" to request.serviceName,
                "price" to request.price,
                "durationMinutes" to request.durationMinutes,
                "appointmentDate" to request.appointmentDate,
                "appointmentTime" to request.appointmentTime,
                "startAt" to Timestamp(Date(startAtMillis)),
                "endAt" to Timestamp(Date(endAtMillis)),
                "status" to AppointmentStatus.UPCOMING.name,
                "slotIds" to slotIds,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            transaction.set(
                appointmentReference,
                appointmentData
            )

            slotReferences.forEach { slotReference ->
                transaction.set(
                    slotReference,
                    hashMapOf(
                        "appointmentId" to appointmentReference.id,
                        "barberId" to request.barberId,
                        "appointmentDate" to request.appointmentDate,
                        "appointmentTime" to request.appointmentTime,
                        "startAt" to Timestamp(Date(startAtMillis)),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            appointmentReference.id
        }.addOnSuccessListener { appointmentId ->
            onResult(Result.success(appointmentId))
        }.addOnFailureListener { error ->
            onResult(Result.failure(error))
        }
    }

    override fun observeCustomerAppointments(
        onResult: (Result<List<Appointment>>) -> Unit
    ): AppointmentObservation? {
        val customerId = auth.currentUser?.uid

        if (customerId == null) {
            onResult(
                Result.failure(
                    AppointmentAuthenticationException()
                )
            )
            return null
        }

        var registration: ListenerRegistration? = null
        var isStopped = false

        firestore
            .collection(USERS_COLLECTION)
            .document(customerId)
            .get()
            .addOnCompleteListener { profileTask ->
                if (isStopped) {
                    return@addOnCompleteListener
                }

                val currentFullName =
                    profileTask
                        .takeIf { task -> task.isSuccessful }
                        ?.result
                        ?.getString("fullName")
                        ?.trim()
                        ?.takeIf(String::isNotBlank)

                registration =
                    firestore
                        .collection(APPOINTMENTS_COLLECTION)
                        .whereEqualTo("customerId", customerId)
                        .orderBy(
                            "startAt",
                            Query.Direction.DESCENDING
                        )
                        .addSnapshotListener { snapshot, error ->
                            if (error != null) {
                                onResult(Result.failure(error))
                                return@addSnapshotListener
                            }

                            val documents =
                                snapshot?.documents.orEmpty()
                            val appointments =
                                documents.mapNotNull(
                                    ::documentToAppointment
                                ).filterNot(
                                    Appointment::hiddenFromCustomer
                                )

                            onResult(Result.success(appointments))

                            if (currentFullName != null) {
                                repairCustomerNames(
                                    documents = documents,
                                    customerId = customerId,
                                    fullName = currentFullName
                                )
                            }
                        }
            }

        return AppointmentObservation {
            isStopped = true
            registration?.remove()
        }
    }

    override fun observeBarberAppointments(
        onResult: (Result<List<Appointment>>) -> Unit
    ): AppointmentObservation? {
        val barberId = auth.currentUser?.uid

        if (barberId == null) {
            onResult(
                Result.failure(
                    AppointmentAuthenticationException()
                )
            )
            return null
        }

        val registration = firestore
            .collection(APPOINTMENTS_COLLECTION)
            .whereEqualTo("barberId", barberId)
            .orderBy("startAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                    return@addSnapshotListener
                }

                val appointments =
                    snapshot
                        ?.documents
                        ?.mapNotNull(::documentToAppointment)
                        ?.filterNot(Appointment::hiddenFromBarber)
                        .orEmpty()

                onResult(Result.success(appointments))
            }

        return AppointmentObservation {
            registration.remove()
        }
    }

    override fun cancelAppointment(
        appointmentId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        updateAppointmentStatus(
            appointmentId = appointmentId,
            newStatus = AppointmentStatus.CANCELLED,
            releaseSlots = true,
            onResult = onResult
        )
    }

    override fun completeAppointment(
        appointmentId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        updateAppointmentStatus(
            appointmentId = appointmentId,
            newStatus = AppointmentStatus.COMPLETED,
            releaseSlots = true,
            onResult = onResult
        )
    }

    override fun hideCustomerAppointments(
        appointmentIds: List<String>,
        onResult: (Result<Unit>) -> Unit
    ) {
        hideHistoryAppointments(appointmentIds, false, onResult)
    }

    override fun hideBarberAppointments(
        appointmentIds: List<String>,
        onResult: (Result<Unit>) -> Unit
    ) {
        hideHistoryAppointments(appointmentIds, true, onResult)
    }

    private fun hideHistoryAppointments(
        appointmentIds: List<String>,
        isBarber: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(
                Result.failure(
                    AppointmentAuthenticationException()
                )
            )
            return
        }

        val uniqueIds = appointmentIds
            .filter(String::isNotBlank)
            .distinct()
        if (uniqueIds.isEmpty()) {
            onResult(Result.success(Unit))
            return
        }

        firestore.runTransaction { transaction ->
            val records = uniqueIds.map { appointmentId ->
                val reference = firestore
                    .collection(APPOINTMENTS_COLLECTION)
                    .document(appointmentId)
                reference to transaction.get(reference)
            }
            records.forEach { (_, appointment) ->
                val allowed = if (isBarber) {
                    AppointmentHistoryPolicy.canBarberHide(
                        authenticatedUserId = userId,
                        appointmentBarberId =
                            appointment.getString("barberId"),
                        appointmentStatus =
                            appointment.getString("status"),
                        alreadyHidden =
                            appointment.getBoolean("hiddenFromBarber") ?: false
                    )
                } else {
                    AppointmentHistoryPolicy.canCustomerHide(
                        authenticatedUserId = userId,
                        appointmentCustomerId =
                            appointment.getString("customerId"),
                        appointmentStatus =
                            appointment.getString("status"),
                        alreadyHidden =
                            appointment.getBoolean("hiddenFromCustomer") ?: false,
                        appointmentEndAtMillis =
                            appointment.getTimestamp("endAt")
                                ?.toDate()
                                ?.time
                    )
                }
                if (!appointment.exists() || !allowed) {
                    throw AppointmentHistoryDeletionException()
                }
            }
            records.forEach { (reference, _) ->
                transaction.update(
                    reference,
                    mapOf(
                        (if (isBarber) "hiddenFromBarber"
                        else "hiddenFromCustomer") to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }.addOnSuccessListener {
            onResult(Result.success(Unit))
        }.addOnFailureListener { error ->
            onResult(Result.failure(error))
        }
    }

    override fun observeAppointment(
        appointmentId: String,
        onResult: (Result<Appointment?>) -> Unit
    ): AppointmentObservation {
        val registration =
            firestore
                .collection(APPOINTMENTS_COLLECTION)
                .document(appointmentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        onResult(
                            Result.success(
                                snapshot
                                    ?.takeIf(DocumentSnapshot::exists)
                                    ?.let(::documentToAppointment)
                            )
                        )
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun rescheduleAppointment(
        request: RescheduleRequest,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            onResult(
                Result.failure(
                    AppointmentAuthenticationException()
                )
            )
            return
        }

        if (
            !AppointmentDateTime.isFuture(
                request.appointmentDate,
                request.appointmentTime
            )
        ) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Choose a future appointment time."
                    )
                )
            )
            return
        }

        val appointmentReference =
            firestore
                .collection(APPOINTMENTS_COLLECTION)
                .document(request.appointmentId)

        firestore.runTransaction { transaction ->
            val appointment =
                transaction.get(appointmentReference)

            if (!appointment.exists()) {
                throw IllegalArgumentException(
                    "This appointment no longer exists."
                )
            }

            if (
                appointment.getString("status") !=
                AppointmentStatus.UPCOMING.name
            ) {
                throw IllegalStateException(
                    "Only upcoming appointments can be rescheduled."
                )
            }

            val customerId =
                appointment.getString("customerId")
            val barberId =
                appointment.getString("barberId")
                    ?: throw IllegalStateException(
                        "The barber is missing."
                    )
            val duration =
                appointment.getLong("durationMinutes")
                    ?.toInt()
                    ?: throw IllegalStateException(
                        "The service duration is missing."
                    )

            if (customerId != userId) {
                throw AppointmentAuthenticationException()
            }

            val availability =
                transaction.get(
                    firestore
                        .collection(AVAILABILITY_COLLECTION)
                        .document(barberId)
                )
            requireBookableSchedule(
                availability = availability,
                date = request.appointmentDate,
                time = request.appointmentTime,
                durationMinutes = duration
            )

            val oldSlotIds =
                (appointment.get("slotIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
            val newSlotIds =
                AppointmentDateTime.slotDocumentIds(
                    barberId = barberId,
                    date = request.appointmentDate,
                    time = request.appointmentTime,
                    durationMinutes = duration
                )
            val newReferences =
                newSlotIds.map { slotId ->
                    firestore
                        .collection(BOOKING_SLOTS_COLLECTION)
                        .document(slotId)
                }
            val occupied =
                newReferences.map { reference ->
                    transaction.get(reference)
                }

            occupied.forEach { slot ->
                val occupyingAppointment =
                    slot.getString("appointmentId")

                if (
                    slot.exists() &&
                    occupyingAppointment != request.appointmentId
                ) {
                    throw BookingConflictException()
                }
            }

            val startAtMillis =
                AppointmentDateTime.toStartMillis(
                    request.appointmentDate,
                    request.appointmentTime
                )
            val endAtMillis =
                startAtMillis + duration * 60_000L

            oldSlotIds
                .filterNot(newSlotIds::contains)
                .forEach { slotId ->
                    transaction.delete(
                        firestore
                            .collection(BOOKING_SLOTS_COLLECTION)
                            .document(slotId)
                    )
                }

            newReferences.forEach { reference ->
                transaction.set(
                    reference,
                    hashMapOf(
                        "appointmentId" to request.appointmentId,
                        "barberId" to barberId,
                        "appointmentDate" to
                            request.appointmentDate,
                        "appointmentTime" to
                            request.appointmentTime,
                        "startAt" to
                            Timestamp(Date(startAtMillis)),
                        "createdAt" to
                            FieldValue.serverTimestamp()
                    )
                )
            }

            transaction.update(
                appointmentReference,
                mapOf(
                    "appointmentDate" to
                        request.appointmentDate,
                    "appointmentTime" to
                        request.appointmentTime,
                    "startAt" to
                        Timestamp(Date(startAtMillis)),
                    "endAt" to
                        Timestamp(Date(endAtMillis)),
                    "slotIds" to newSlotIds,
                    "rescheduledAt" to
                        FieldValue.serverTimestamp(),
                    "updatedAt" to
                        FieldValue.serverTimestamp()
                )
            )
        }.addOnSuccessListener {
            onResult(Result.success(Unit))
        }.addOnFailureListener { error ->
            onResult(Result.failure(error))
        }
    }

    private fun updateAppointmentStatus(
        appointmentId: String,
        newStatus: AppointmentStatus,
        releaseSlots: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(
                Result.failure(
                    AppointmentAuthenticationException()
                )
            )
            return
        }

        val appointmentReference =
            firestore
                .collection(APPOINTMENTS_COLLECTION)
                .document(appointmentId)

        firestore.runTransaction { transaction ->
            val snapshot =
                transaction.get(appointmentReference)

            if (!snapshot.exists()) {
                throw IllegalArgumentException(
                    "This appointment no longer exists."
                )
            }

            val currentStatus =
                AppointmentStatus.fromFirestore(
                    snapshot.getString("status")
                )

            if (currentStatus != AppointmentStatus.UPCOMING) {
                throw IllegalStateException(
                    "Only upcoming appointments can be updated."
                )
            }

            val slotIds =
                (snapshot.get("slotIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()

            transaction.update(
                appointmentReference,
                mapOf(
                    "status" to newStatus.name,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            if (
                newStatus == AppointmentStatus.CANCELLED &&
                snapshot.getString("barberId") == userId
            ) {
                val customerId =
                    snapshot.getString("customerId")
                        ?: throw IllegalStateException(
                            "The appointment customer is invalid."
                        )
                val barberId =
                    snapshot.getString("barberId")
                        ?: throw IllegalStateException(
                            "The appointment barber is invalid."
                        )
                val notificationId =
                    "cancelled_" + appointmentId
                transaction.set(
                    firestore
                        .collection(USERS_COLLECTION)
                        .document(customerId)
                        .collection(NOTIFICATIONS_COLLECTION)
                        .document(notificationId),
                    hashMapOf(
                        "notificationId" to notificationId,
                        "userId" to customerId,
                        "type" to "APPOINTMENT_CANCELLED",
                        "title" to "Appointment cancelled",
                        "message" to "Cancelled by " +
                            snapshot.getString("barberName").orEmpty() + ".",
                        "appointmentId" to appointmentId,
                        "barberId" to barberId,
                        "isRead" to false,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            if (releaseSlots) {
                slotIds.forEach { slotId ->
                    transaction.delete(
                        firestore
                            .collection(BOOKING_SLOTS_COLLECTION)
                            .document(slotId)
                    )
                }
            }
        }.addOnSuccessListener {
            onResult(Result.success(Unit))
        }.addOnFailureListener { error ->
            onResult(Result.failure(error))
        }
    }

    private fun requireBookableSchedule(
        availability: DocumentSnapshot,
        date: String,
        time: String,
        durationMinutes: Int
    ) {
        if (!availability.exists()) {
            throw BookingUnavailableException()
        }
        val rawDays = availability.get("days") as? List<*>
            ?: throw BookingUnavailableException()
        if (rawDays.size != 7) {
            throw BookingUnavailableException()
        }
        val rawDayMaps = rawDays.mapNotNull { raw ->
            @Suppress("UNCHECKED_CAST")
            raw as? Map<String, Any?>
        }
        val (scheduleSaved, hasOpenDay) =
            BarberShopReadinessEvaluator.availabilityState(
                exists = true,
                rawDays = rawDayMaps
            )
        if (!scheduleSaved || !hasOpenDay) {
            throw BookingUnavailableException()
        }
        val schedule = BarberAvailabilityDocumentCodec.decode(
            rawDays = rawDays,
            rawBlockedDates = availability.get("blockedDates")
        )
        if (
            schedule.days.size != 7 ||
            BarberManagementValidator
                .validateAvailability(schedule) != null
        ) {
            throw BookingUnavailableException()
        }
        val parsedDate = runCatching { LocalDate.parse(date) }
            .getOrNull()
            ?: throw BookingUnavailableException()
        val parsedTime = runCatching { LocalTime.parse(time) }
            .getOrNull()
            ?: throw BookingUnavailableException()

        if (
            !BarberManagementValidator.isBookable(
                availability = schedule,
                date = parsedDate,
                time = parsedTime,
                durationMinutes = durationMinutes
            )
        ) {
            throw BookingUnavailableException()
        }
    }

    private fun repairCustomerNames(
        documents: List<DocumentSnapshot>,
        customerId: String,
        fullName: String
    ) {
        val staleAppointments =
            documents.filter { document ->
                document.getString("customerId") == customerId &&
                    document.getString("customerName") != fullName
            }

        if (staleAppointments.isEmpty()) {
            return
        }

        staleAppointments
            .chunked(FIRESTORE_BATCH_WRITE_LIMIT)
            .forEach { chunk ->
                firestore.batch()
                    .apply {
                        chunk.forEach { document ->
                            update(
                                document.reference,
                                mapOf(
                                    "customerName" to fullName,
                                    "updatedAt" to
                                        FieldValue.serverTimestamp()
                                )
                            )
                        }
                    }
                    .commit()
            }
    }

    private fun documentToAppointment(
        document: DocumentSnapshot
    ): Appointment? {
        val startAt =
            document.getTimestamp("startAt")
                ?: return null
        val endAt =
            document.getTimestamp("endAt")
                ?: return null

        return Appointment(
            id =
                document.getString("appointmentId")
                    ?: document.id,
            customerId =
                document.getString("customerId")
                    ?: return null,
            customerName =
                document.getString("customerName")
                    ?: "Customer",
            customerEmail =
                document.getString("customerEmail").orEmpty(),
            barberId =
                document.getString("barberId")
                    ?: return null,
            barberName =
                document.getString("barberName")
                    ?: "Barber",
            serviceId =
                document.getString("serviceId")
                    ?: return null,
            serviceName =
                document.getString("serviceName")
                    ?: "Service",
            price =
                document.getLong("price")?.toInt()
                    ?: 0,
            durationMinutes =
                document.getLong("durationMinutes")?.toInt()
                    ?: 0,
            appointmentDate =
                document.getString("appointmentDate")
                    ?: return null,
            appointmentTime =
                document.getString("appointmentTime")
                    ?: return null,
            startAtMillis = startAt.toDate().time,
            endAtMillis = endAt.toDate().time,
            status =
                AppointmentStatus.fromFirestore(
                    document.getString("status")
                ),
            hiddenFromCustomer =
                document.getBoolean("hiddenFromCustomer")
                    ?: false,
            hiddenFromBarber =
                document.getBoolean("hiddenFromBarber")
                    ?: false,
            ratingId = document.getString("ratingId"),
            createdAtMillis =
                document.getTimestamp("createdAt")
                    ?.toDate()?.time ?: 0L,
            updatedAtMillis =
                document.getTimestamp("updatedAt")
                    ?.toDate()?.time ?: 0L,
            rescheduledAtMillis =
                document.getTimestamp("rescheduledAt")
                    ?.toDate()?.time
        )
    }

    private companion object {
        const val APPOINTMENTS_COLLECTION = "appointments"
        const val BOOKING_SLOTS_COLLECTION = "bookingSlots"
        const val USERS_COLLECTION = "users"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val AVAILABILITY_COLLECTION = "barberAvailability"
        const val FIRESTORE_BATCH_WRITE_LIMIT = 500
    }
}
