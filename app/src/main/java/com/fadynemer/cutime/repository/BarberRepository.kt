package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.BarberAvailability
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.DayAvailability
import com.fadynemer.cutime.model.ManagedBarberProfile
import com.fadynemer.cutime.model.defaultWorkingWeek
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Query

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
        onResult: (Result<Unit>) -> Unit
    )
}

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
        onResult: (Result<Unit>) -> Unit
    ) {
        val uid = requireUid(onResult) ?: return
        val dayMaps =
            availability.days.map { day ->
                hashMapOf(
                    "day" to day.day,
                    "isOpen" to day.isOpen,
                    "startTime" to day.startTime,
                    "endTime" to day.endTime
                )
            }

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
                    "days" to dayMaps,
                    "blockedDates" to
                        availability.blockedDates.distinct().sorted(),
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
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
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
        val rawDays =
            document.get("days") as? List<*>
        val days =
            rawDays
                ?.mapNotNull { value ->
                    val map = value as? Map<*, *>
                        ?: return@mapNotNull null

                    DayAvailability(
                        day =
                            map["day"] as? String
                                ?: return@mapNotNull null,
                        isOpen =
                            map["isOpen"] as? Boolean
                                ?: false,
                        startTime =
                            map["startTime"] as? String
                                ?: "09:00",
                        endTime =
                            map["endTime"] as? String
                                ?: "17:00"
                    )
                }
                ?.takeIf(List<DayAvailability>::isNotEmpty)
                ?: defaultWorkingWeek()

        val blockedDates =
            (document.get("blockedDates") as? List<*>)
                ?.filterIsInstance<String>()
                .orEmpty()

        return BarberAvailability(
            days = days,
            blockedDates = blockedDates
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
    }
}
