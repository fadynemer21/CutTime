package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.AccountDeletionRequest
import com.fadynemer.cutime.model.AccountDeletionStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

interface AccountDeletionDataSource {
    fun observeRequest(
        onResult: (Result<AccountDeletionRequest?>) -> Unit
    ): AppointmentObservation?

    fun submitRequest(
        role: String,
        onResult: (Result<Unit>) -> Unit
    )
}

class AccountDeletionRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) : AccountDeletionDataSource {

    override fun observeRequest(
        onResult: (Result<AccountDeletionRequest?>) -> Unit
    ): AppointmentObservation? {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(Result.failure(IllegalStateException(LOGIN_REQUIRED)))
            return null
        }

        val registration = requestReference(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(Result.failure(error))
                } else {
                    onResult(
                        Result.success(
                            snapshot
                                ?.takeIf(DocumentSnapshot::exists)
                                ?.let(::mapRequest)
                        )
                    )
                }
            }
        return AppointmentObservation(registration::remove)
    }

    override fun submitRequest(
        role: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val user = auth.currentUser
        val userId = user?.uid
        val email = user?.email
        if (userId == null || email.isNullOrBlank()) {
            onResult(Result.failure(IllegalStateException(LOGIN_REQUIRED)))
            return
        }
        if (role !in setOf("CUSTOMER", "BARBER")) {
            onResult(Result.failure(IllegalArgumentException(INVALID_ROLE)))
            return
        }

        requestReference(userId)
            .set(
                mapOf(
                    "userId" to userId,
                    "email" to email,
                    "role" to role,
                    "status" to AccountDeletionStatus.PENDING.name,
                    "requestedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun requestReference(userId: String) =
        firestore.collection(REQUESTS_COLLECTION).document(userId)

    private fun mapRequest(
        document: DocumentSnapshot
    ): AccountDeletionRequest? {
        return AccountDeletionRequest(
            userId = document.getString("userId") ?: return null,
            email = document.getString("email") ?: return null,
            role = document.getString("role") ?: return null,
            status = AccountDeletionStatus.fromFirestore(
                document.getString("status")
            ),
            requestedAtMillis =
                document.getTimestamp("requestedAt")
                    ?.toDate()?.time ?: 0L
        )
    }

    private companion object {
        const val REQUESTS_COLLECTION = "accountDeletionRequests"
        const val LOGIN_REQUIRED = "Please log in again to continue."
        const val INVALID_ROLE = "The account role is invalid."
    }
}
