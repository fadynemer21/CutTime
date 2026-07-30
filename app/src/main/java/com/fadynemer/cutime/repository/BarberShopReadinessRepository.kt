package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.BarberShopReadiness
import com.fadynemer.cutime.model.BarberShopReadinessEvaluator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

interface BarberShopReadinessDataSource {
    fun observeReadiness(
        onResult: (Result<BarberShopReadiness>) -> Unit
    ): AppointmentObservation?
}

class BarberShopReadinessRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) : BarberShopReadinessDataSource {

    override fun observeReadiness(
        onResult: (Result<BarberShopReadiness>) -> Unit
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

        val state = CombinedReadinessState(onResult)
        val registrations = mutableListOf<ListenerRegistration>()

        registrations +=
            profileReference(barberId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        state.fail(error)
                    } else {
                        state.updateProfile(snapshot)
                    }
                }
        registrations +=
            profileReference(barberId)
                .collection(SERVICES_COLLECTION)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        state.fail(error)
                    } else {
                        val rawServices =
                            snapshot?.documents
                                ?.mapNotNull { document ->
                                    document.data
                                }
                                .orEmpty()
                        state.updateServices(rawServices)
                    }
                }
        registrations +=
            firestore
                .collection(AVAILABILITY_COLLECTION)
                .document(barberId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        state.fail(error)
                    } else {
                        state.updateAvailability(snapshot)
                    }
                }

        return AppointmentObservation {
            registrations.forEach(
                ListenerRegistration::remove
            )
        }
    }

    private fun profileReference(barberId: String) =
        firestore.collection(PROFILES_COLLECTION)
            .document(barberId)

    private class CombinedReadinessState(
        private val onResult:
            (Result<BarberShopReadiness>) -> Unit
    ) {
        private var profileComplete: Boolean? = null
        private var validServiceCount: Int? = null
        private var availabilitySaved: Boolean? = null
        private var hasOpenWorkingDay: Boolean? = null

        @Synchronized
        fun updateProfile(snapshot: DocumentSnapshot?) {
            profileComplete =
                snapshot
                    ?.takeIf(DocumentSnapshot::exists)
                    ?.let { document ->
                        BarberShopReadinessEvaluator
                            .profileComplete(
                                shopName =
                                    document.getString("shopName"),
                                description =
                                    document.getString(
                                        "description"
                                    )
                            )
                    }
                    ?: false
            emitIfReady()
        }

        @Synchronized
        fun updateServices(
            services: List<Map<String, Any?>>
        ) {
            validServiceCount =
                BarberShopReadinessEvaluator
                    .validServiceCount(services)
            emitIfReady()
        }

        @Synchronized
        fun updateAvailability(
            snapshot: DocumentSnapshot?
        ) {
            val rawDays =
                (
                    snapshot?.get("days") as? List<*>
                    )
                    ?.mapNotNull { value ->
                        @Suppress("UNCHECKED_CAST")
                        value as? Map<String, Any?>
                    }
                    .orEmpty()
            val (saved, hasOpenDay) =
                BarberShopReadinessEvaluator.availabilityState(
                    exists = snapshot?.exists() == true,
                    rawDays = rawDays
                )
            availabilitySaved = saved
            hasOpenWorkingDay = hasOpenDay
            emitIfReady()
        }

        @Synchronized
        fun fail(error: Throwable) {
            onResult(Result.failure(error))
        }

        private fun emitIfReady() {
            val profile = profileComplete ?: return
            val serviceCount = validServiceCount ?: return
            val availability = availabilitySaved ?: return
            val openDay = hasOpenWorkingDay ?: return

            onResult(
                Result.success(
                    BarberShopReadiness(
                        profileComplete = profile,
                        validServiceCount = serviceCount,
                        availabilitySaved = availability,
                        hasOpenWorkingDay = openDay
                    )
                )
            )
        }
    }

    private companion object {
        const val PROFILES_COLLECTION = "barberProfiles"
        const val SERVICES_COLLECTION = "services"
        const val AVAILABILITY_COLLECTION =
            "barberAvailability"
    }
}
