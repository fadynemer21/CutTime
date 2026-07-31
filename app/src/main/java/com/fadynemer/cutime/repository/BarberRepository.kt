package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberAvailabilityDocumentCodec
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.AvailabilitySaveResult
import com.fadynemer.cutime.model.ManagedBarberProfile
import com.fadynemer.cutime.util.HolidayCancellationPolicy
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

interface BarberDataSource {
    fun observeProfile(
        onResult: (Result<ManagedBarberProfile?>) -> Unit
    ): AppointmentObservation?

    fun saveProfile(
        profile: ManagedBarberProfile,
        onResult: (Result<Unit>) -> Unit
    )

    fun observeServices(
        onResult: (Result<List<BarberService>>) -> Unit
    ): AppointmentObservation?

    fun saveService(
        service: BarberService,
        onResult: (Result<String>) -> Unit
    )

    fun deleteService(
        serviceId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun observeAvailability(
        onResult: (Result<BarberAvailability>) -> Unit
    ): AppointmentObservation?

    fun saveAvailability(
        availability: BarberAvailability,
        onResult: (Result<AvailabilitySaveResult>) -> Unit
    )
}

class HolidayCancellationLimitException :
    Exception(
        "Too many appointments are affected at once. Block fewer dates and try again."
    )

class HolidayCancellationException(
    cause: Throwable
) : Exception(
    "The holiday was saved, but its appointments could not be cancelled. Save again to retry.",
    cause
)

class BarberRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) : BarberDataSource {

    override fun observeProfile(
        onResult: (Result<ManagedBarberProfile?>) -> Unit
    ): AppointmentObservation? {
        val uid = requireUid(onResult) ?: return null

        val registration =
            profileReference(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        onResult(
                            Result.success(
                                snapshot
                                    ?.takeIf(DocumentSnapshot::exists)
                                    ?.let(::profileFromDocument)
                            )
                        )
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun saveProfile(
        profile: ManagedBarberProfile,
        onResult: (Result<Unit>) -> Unit
    ) {
        val uid = requireUid(onResult) ?: return

        profileReference(uid)
            .set(
                hashMapOf(
                    "uid" to uid,
                    "shopName" to profile.shopName.trim(),
                    "description" to profile.description.trim(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                SetOptions.merge()
            )
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun observeServices(
        onResult: (Result<List<BarberService>>) -> Unit
    ): AppointmentObservation? {
        val uid = requireUid(onResult) ?: return null

        val registration =
            servicesCollection(uid)
                .orderBy("name", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        onResult(
                            Result.success(
                                snapshot
                                    ?.documents
                                    ?.mapNotNull(::serviceFromDocument)
                                    .orEmpty()
                            )
                        )
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun saveService(
        service: BarberService,
        onResult: (Result<String>) -> Unit
    ) {
        val uid = requireUid(onResult) ?: return
        val reference =
            if (service.id.isBlank()) {
                servicesCollection(uid).document()
            } else {
                servicesCollection(uid).document(service.id)
            }

        firestore.batch()
            .apply {
                set(
                    reference,
                hashMapOf(
                    "serviceId" to reference.id,
                    "barberId" to uid,
                    "name" to service.name.trim(),
                    "price" to service.price,
                    "durationMinutes" to service.durationMinutes,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                )
                update(
                    profileReference(uid),
                    "updatedAt",
                    FieldValue.serverTimestamp()
                )
            }
            .commit()
            .addOnSuccessListener {
                onResult(Result.success(reference.id))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun deleteService(
        serviceId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val uid = requireUid(onResult) ?: return

        firestore.batch()
            .apply {
                delete(
                    servicesCollection(uid).document(serviceId)
                )
                update(
                    profileReference(uid),
                    "updatedAt",
                    FieldValue.serverTimestamp()
                )
            }
            .commit()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun observeAvailability(
        onResult: (Result<BarberAvailability>) -> Unit
    ): AppointmentObservation? {
        val uid = requireUid(onResult) ?: return null

        val registration =
            firestore
                .collection(AVAILABILITY_COLLECTION)
                .document(uid)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        onResult(
                            Result.success(
                                snapshot
                                    ?.takeIf(DocumentSnapshot::exists)
                                    ?.let(::availabilityFromDocument)
                                    ?: BarberAvailability()
                            )
                        )
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun saveAvailability(
        availability: BarberAvailability,
        onResult: (Result<AvailabilitySaveResult>) -> Unit
    ) {
        val uid = requireUid(onResult) ?: return
        val blockedDates =
            availability.blockedDates.toSet()

        persistAvailabilityDocument(
            uid = uid,
            availability = availability,
            onResult = { result ->
                result
                    .onSuccess {
                        if (blockedDates.isEmpty()) {
                            onResult(
                                Result.success(
                                    AvailabilitySaveResult()
                                )
                            )
                        } else {
                            loadAppointmentsOnBlockedDates(
                                uid = uid,
                                blockedDates = blockedDates,
                                onResult = { appointmentsResult ->
                                    appointmentsResult
                                        .onSuccess { appointments ->
                                            cancelBlockedAppointments(
                                                appointments =
                                                    appointments,
                                                blockedDates =
                                                    blockedDates,
                                                onResult = onResult
                                            )
                                        }
                                        .onFailure { error ->
                                            onResult(
                                                Result.failure(
                                                    HolidayCancellationException(
                                                        error
                                                    )
                                                )
                                            )
                                        }
                                }
                            )
                        }
                    }
                    .onFailure { error ->
                        onResult(Result.failure(error))
                    }
            }
        )
    }

    private fun persistAvailabilityDocument(
        uid: String,
        availability: BarberAvailability,
        onResult: (Result<Unit>) -> Unit
    ) {
        val dayMaps =
            availability.days.map(
                BarberAvailabilityDocumentCodec::encodeDay
            )
        val availabilityReference =
            firestore
                .collection(AVAILABILITY_COLLECTION)
                .document(uid)

        firestore.batch()
            .apply {
                set(
                    availabilityReference,
                    hashMapOf(
                        "barberId" to uid,
                        "schemaVersion" to
                            BarberAvailabilityDocumentCodec
                                .CURRENT_SCHEMA_VERSION,
                        "days" to dayMaps,
                        "blockedDates" to
                            availability.blockedDates
                                .distinct()
                                .sorted(),
                        "updatedAt" to
                            FieldValue.serverTimestamp()
                    )
                )
                update(
                    profileReference(uid),
                    "updatedAt",
                    FieldValue.serverTimestamp()
                )
            }
            .commit()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun loadAppointmentsOnBlockedDates(
        uid: String,
        blockedDates: Set<String>,
        onResult: (Result<QuerySnapshot>) -> Unit
    ) {
        val parsedDates =
            blockedDates.map(LocalDate::parse)
        val zoneId = ZoneId.systemDefault()
        val rangeStart =
            parsedDates.min()
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()
        val rangeEnd =
            parsedDates.max()
                .plusDays(1)
                .atStartOfDay(zoneId)
                .toInstant()
                .toEpochMilli()

        firestore
            .collection(APPOINTMENTS_COLLECTION)
            .whereEqualTo("barberId", uid)
            .whereGreaterThanOrEqualTo(
                "startAt",
                Timestamp(Date(rangeStart))
            )
            .whereLessThan(
                "startAt",
                Timestamp(Date(rangeEnd))
            )
            .get()
            .addOnSuccessListener { snapshot ->
                onResult(Result.success(snapshot))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun cancelBlockedAppointments(
        appointments: QuerySnapshot,
        blockedDates: Set<String>,
        onResult: (Result<AvailabilitySaveResult>) -> Unit
    ) {
        val appointmentsToCancel =
            appointments.documents
                .filter { document ->
                    HolidayCancellationPolicy.shouldCancel(
                        status = document.getString("status"),
                        appointmentDate =
                            document.getString("appointmentDate"),
                        blockedDates = blockedDates
                    )
                }

        if (appointmentsToCancel.isEmpty()) {
            onResult(
                Result.success(
                    AvailabilitySaveResult()
                )
            )
            return
        }

        commitCancellationChunks(
            chunks =
                appointmentsToCancel.chunked(
                    HOLIDAY_CANCELLATION_CHUNK_SIZE
                ),
            chunkIndex = 0,
            cancelledCount = 0,
            onResult = onResult
        )
    }

    private fun commitCancellationChunks(
        chunks: List<List<DocumentSnapshot>>,
        chunkIndex: Int,
        cancelledCount: Int,
        onResult: (Result<AvailabilitySaveResult>) -> Unit
    ) {
        if (chunkIndex >= chunks.size) {
            onResult(
                Result.success(
                    AvailabilitySaveResult(
                        cancelledAppointmentCount = cancelledCount
                    )
                )
            )
            return
        }

        val chunk = chunks[chunkIndex]
        val slotIdsByAppointment =
            chunk.associateWith { appointment ->
                (appointment.get("slotIds") as? List<*>)
                    ?.filterIsInstance<String>()
                    .orEmpty()
            }
        val writeCount =
            HolidayCancellationPolicy.requiredBatchWrites(
                affectedSlotCounts =
                    slotIdsByAppointment.values.map(List<String>::size),
                baseWrites = 0
            )

        if (writeCount > FIRESTORE_BATCH_WRITE_LIMIT) {
            onResult(
                Result.failure(
                    HolidayCancellationLimitException()
                )
            )
            return
        }

        firestore.batch()
            .apply {
                chunk.forEach { appointment ->
                    update(
                        appointment.reference,
                        mapOf(
                            "status" to "CANCELLED",
                            "updatedAt" to
                                FieldValue.serverTimestamp()
                        )
                    )
                    slotIdsByAppointment
                        .getValue(appointment)
                        .forEach { slotId ->
                            delete(
                                firestore
                                    .collection(
                                        BOOKING_SLOTS_COLLECTION
                                    )
                                    .document(slotId)
                            )
                        }
                }
            }
            .commit()
            .addOnSuccessListener {
                commitCancellationChunks(
                    chunks = chunks,
                    chunkIndex = chunkIndex + 1,
                    cancelledCount =
                        cancelledCount + chunk.size,
                    onResult = onResult
                )
            }
            .addOnFailureListener { error ->
                onResult(
                    Result.failure(
                        HolidayCancellationException(error)
                    )
                )
            }
    }

    private fun profileReference(uid: String) =
        firestore
            .collection(PROFILES_COLLECTION)
            .document(uid)

    private fun servicesCollection(uid: String) =
        profileReference(uid)
            .collection(SERVICES_COLLECTION)

    private fun profileFromDocument(
        document: DocumentSnapshot
    ) = ManagedBarberProfile(
        uid = document.getString("uid").orEmpty(),
        shopName = document.getString("shopName").orEmpty(),
        description = document.getString("description").orEmpty()
    )

    private fun serviceFromDocument(
        document: DocumentSnapshot
    ): BarberService? {
        return BarberService(
            id =
                document.getString("serviceId")
                    ?: document.id,
            name =
                document.getString("name")
                    ?: return null,
            price =
                document.getLong("price")?.toInt()
                    ?: return null,
            durationMinutes =
                document.getLong("durationMinutes")?.toInt()
                    ?: return null
        )
    }

    private fun availabilityFromDocument(
        document: DocumentSnapshot
    ): BarberAvailability {
        return BarberAvailabilityDocumentCodec.decode(
            rawDays = document.get("days"),
            rawBlockedDates = document.get("blockedDates")
        )
    }

    private fun <T> requireUid(
        onResult: (Result<T>) -> Unit
    ): String? {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            onResult(
                Result.failure(
                    AppointmentAuthenticationException()
                )
            )
        }

        return uid
    }

    private companion object {
        const val PROFILES_COLLECTION = "barberProfiles"
        const val SERVICES_COLLECTION = "services"
        const val AVAILABILITY_COLLECTION = "barberAvailability"
        const val APPOINTMENTS_COLLECTION = "appointments"
        const val BOOKING_SLOTS_COLLECTION = "bookingSlots"
        const val HOLIDAY_CANCELLATION_CHUNK_SIZE = 10
        const val FIRESTORE_BATCH_WRITE_LIMIT = 500
    }
}
