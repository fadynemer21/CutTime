package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.model.Rating
import com.fadynemer.cutime.model.RatingRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class RatingEligibilityException(
    message: String
) : Exception(message)

interface RatingDataSource {
    fun submitRating(
        request: RatingRequest,
        onResult: (Result<Unit>) -> Unit
    )

    fun observeBarberRatings(
        barberId: String,
        onResult: (Result<List<Rating>>) -> Unit
    ): AppointmentObservation

    fun observeAppointmentRating(
        appointmentId: String,
        onResult: (Result<Rating?>) -> Unit
    ): AppointmentObservation
}

class RatingRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) : RatingDataSource {

    override fun submitRating(
        request: RatingRequest,
        onResult: (Result<Unit>) -> Unit
    ) {
        val customer = auth.currentUser

        if (customer == null) {
            onResult(
                Result.failure(
                    AppointmentAuthenticationException()
                )
            )
            return
        }

        val validationError = validate(request)

        if (validationError != null) {
            onResult(
                Result.failure(
                    IllegalArgumentException(validationError)
                )
            )
            return
        }

        val appointmentReference =
            firestore
                .collection(APPOINTMENTS_COLLECTION)
                .document(request.appointmentId)
        val ratingReference =
            firestore
                .collection(RATINGS_COLLECTION)
                .document(request.appointmentId)
        val barberReference =
            firestore
                .collection(PROFILES_COLLECTION)
                .document(request.barberId)

        firestore.runTransaction { transaction ->
            val appointment =
                transaction.get(appointmentReference)
            val existingRating =
                transaction.get(ratingReference)
            val barber =
                transaction.get(barberReference)

            if (!appointment.exists()) {
                throw RatingEligibilityException(
                    "The appointment no longer exists."
                )
            }

            if (
                appointment.getString("customerId") !=
                customer.uid
            ) {
                throw RatingEligibilityException(
                    "Only the booking customer can rate this appointment."
                )
            }

            if (
                appointment.getString("barberId") !=
                request.barberId
            ) {
                throw RatingEligibilityException(
                    "The selected barber does not match the appointment."
                )
            }

            if (
                appointment.getString("status") !=
                AppointmentStatus.COMPLETED.name
            ) {
                throw RatingEligibilityException(
                    "Ratings are available after a completed appointment."
                )
            }

            if (existingRating.exists()) {
                throw RatingEligibilityException(
                    "This appointment has already been rated."
                )
            }

            if (!barber.exists()) {
                throw RatingEligibilityException(
                    "The barber profile no longer exists."
                )
            }

            val previousCount =
                barber.getLong("ratingCount") ?: 0L
            val previousSum =
                barber.getLong("ratingSum") ?: 0L
            val newCount = previousCount + 1L
            val newSum = previousSum + request.stars
            val newAverage =
                newSum.toDouble() / newCount.toDouble()

            transaction.set(
                ratingReference,
                hashMapOf(
                    "ratingId" to request.appointmentId,
                    "appointmentId" to request.appointmentId,
                    "customerId" to customer.uid,
                    "barberId" to request.barberId,
                    "customerName" to
                        (
                            customer.displayName
                                ?: customer.email
                                ?: "Customer"
                            ),
                    "stars" to request.stars,
                    "review" to request.review.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )

            transaction.update(
                barberReference,
                mapOf(
                    "ratingCount" to newCount,
                    "ratingSum" to newSum,
                    "ratingAverage" to newAverage,
                    "lastRatingId" to request.appointmentId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            transaction.update(
                appointmentReference,
                mapOf(
                    "ratingId" to request.appointmentId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.addOnSuccessListener {
            onResult(Result.success(Unit))
        }.addOnFailureListener { error ->
            onResult(Result.failure(error))
        }
    }

    override fun observeBarberRatings(
        barberId: String,
        onResult: (Result<List<Rating>>) -> Unit
    ): AppointmentObservation {
        val registration =
            firestore
                .collection(RATINGS_COLLECTION)
                .whereEqualTo("barberId", barberId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(50)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        onResult(
                            Result.success(
                                snapshot
                                    ?.documents
                                    ?.mapNotNull(::mapRating)
                                    .orEmpty()
                            )
                        )
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun observeAppointmentRating(
        appointmentId: String,
        onResult: (Result<Rating?>) -> Unit
    ): AppointmentObservation {
        val registration =
            firestore
                .collection(RATINGS_COLLECTION)
                .document(appointmentId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        onResult(
                            Result.success(
                                snapshot
                                    ?.takeIf(DocumentSnapshot::exists)
                                    ?.let(::mapRating)
                            )
                        )
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    private fun mapRating(
        document: DocumentSnapshot
    ): Rating? {
        return Rating(
            id =
                document.getString("ratingId")
                    ?: document.id,
            appointmentId =
                document.getString("appointmentId")
                    ?: return null,
            customerId =
                document.getString("customerId")
                    ?: return null,
            barberId =
                document.getString("barberId")
                    ?: return null,
            customerName =
                document.getString("customerName")
                    ?: "Customer",
            stars =
                document.getLong("stars")?.toInt()
                    ?: return null,
            review =
                document.getString("review").orEmpty(),
            createdAtMillis =
                document.getTimestamp("createdAt")
                    ?.toDate()?.time ?: 0L
        )
    }

    private fun validate(
        request: RatingRequest
    ): String? {
        return when {
            request.appointmentId.isBlank() ->
                "The appointment is invalid."

            request.barberId.isBlank() ->
                "The barber is invalid."

            request.stars !in 1..5 ->
                "Choose a rating from 1 to 5 stars."

            request.review.length > 500 ->
                "Keep the review under 500 characters."

            else -> null
        }
    }

    private companion object {
        const val APPOINTMENTS_COLLECTION = "appointments"
        const val RATINGS_COLLECTION = "ratings"
        const val PROFILES_COLLECTION = "barberProfiles"
    }
}
