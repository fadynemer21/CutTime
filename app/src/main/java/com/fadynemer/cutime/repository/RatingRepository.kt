package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.model.Rating
import com.fadynemer.cutime.model.RatingRequest
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.UUID

class RatingEligibilityException(
    message: String
) : Exception(message)

interface RatingDataSource {
    fun submitRating(
        request: RatingRequest,
        onResult: (Result<Unit>) -> Unit
    )

    fun deleteRating(
        appointmentId: String,
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
        val customerProfileReference =
            firestore
                .collection(USERS_COLLECTION)
                .document(customer.uid)
        val notificationId = "review_" + UUID.randomUUID()
        val barberNotificationReference =
            firestore
                .collection(USERS_COLLECTION)
                .document(request.barberId)
                .collection(NOTIFICATIONS_COLLECTION)
                .document(notificationId)

        firestore.runTransaction { transaction ->
            val appointment =
                transaction.get(appointmentReference)
            val existingRating =
                transaction.get(ratingReference)
            val customerProfile =
                transaction.get(customerProfileReference)

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

            if (customerProfile.getString("role") != "CUSTOMER") {
                throw RatingEligibilityException(
                    "Only customer accounts can submit ratings."
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

            transaction.set(
                ratingReference,
                hashMapOf(
                    "ratingId" to request.appointmentId,
                    "appointmentId" to request.appointmentId,
                    "customerId" to customer.uid,
                    "barberId" to request.barberId,
                    "customerName" to customerProfile
                        .getString("fullName")
                        ?.trim()
                        .orEmpty(),
                    "notificationId" to notificationId,
                    "stars" to request.stars,
                    "review" to request.review.trim(),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            val customerName = customerProfile
                .getString("fullName")
                ?.trim()
                .orEmpty()
            transaction.set(
                barberNotificationReference,
                hashMapOf(
                    "notificationId" to notificationId,
                    "userId" to request.barberId,
                    "type" to "GENERAL",
                    "title" to "New review",
                    "message" to customerName +
                        " left a review.",
                    "appointmentId" to request.appointmentId,
                    "barberId" to request.barberId,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.addOnSuccessListener {
            appointmentReference.update(
                mapOf(
                    "ratingId" to request.appointmentId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).addOnCompleteListener {
                onResult(Result.success(Unit))
            }
        }.addOnFailureListener { error ->
            onResult(Result.failure(error))
        }
    }

    override fun deleteRating(
        appointmentId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val customerId = auth.currentUser?.uid
        if (customerId == null) {
            onResult(Result.failure(AppointmentAuthenticationException()))
            return
        }
        if (appointmentId.isBlank()) {
            onResult(
                Result.failure(
                    IllegalArgumentException("The review is invalid.")
                )
            )
            return
        }

        val ratingReference = firestore
            .collection(RATINGS_COLLECTION)
            .document(appointmentId)
        val appointmentReference = firestore
            .collection(APPOINTMENTS_COLLECTION)
            .document(appointmentId)

        ratingReference.delete()
            .addOnSuccessListener {
                appointmentReference.update(
                    mapOf(
                        "ratingId" to FieldValue.delete(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).addOnCompleteListener {
                    onResult(Result.success(Unit))
                }
            }
            .addOnFailureListener { error ->
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
                        val ratings = snapshot
                            ?.documents
                            ?.mapNotNull(::mapRating)
                            .orEmpty()
                        resolveCustomerNames(
                            ratings = ratings,
                            onResult = onResult
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

    private fun resolveCustomerNames(
        ratings: List<Rating>,
        onResult: (Result<List<Rating>>) -> Unit
    ) {
        val unresolved = ratings.filter {
            it.customerName.isBlank() ||
                '@' in it.customerName ||
                it.customerName.equals("Customer", ignoreCase = true)
        }
        if (unresolved.isEmpty()) {
            onResult(Result.success(ratings))
            return
        }

        val appointmentTasks = unresolved.map { rating ->
            firestore
                .collection(APPOINTMENTS_COLLECTION)
                .document(rating.appointmentId)
                .get()
        }
        Tasks.whenAllComplete(appointmentTasks)
            .addOnCompleteListener {
                val namesByAppointment = unresolved
                    .zip(appointmentTasks)
                    .mapNotNull { (rating, task) ->
                        val appointment =
                            task.takeIf { it.isSuccessful }?.result
                        val name = appointment
                            ?.getString("customerName")
                            ?.trim()
                            ?.takeIf(String::isNotBlank)
                            ?: return@mapNotNull null
                        rating.appointmentId to name
                    }.toMap()
                val resolvedRatings = ratings.map { rating ->
                    val resolved =
                        namesByAppointment[rating.appointmentId]
                    if (resolved.isNullOrBlank()) {
                        rating
                    } else {
                        if (resolved != rating.customerName) {
                            firestore
                                .collection(RATINGS_COLLECTION)
                                .document(rating.id)
                                .update("customerName", resolved)
                        }
                        rating.copy(customerName = resolved)
                    }
                }
                onResult(Result.success(resolvedRatings))
            }
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
        const val USERS_COLLECTION = "users"
        const val NOTIFICATIONS_COLLECTION = "notifications"
    }
}
