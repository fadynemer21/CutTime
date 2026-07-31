package com.fadynemer.cutime.model

data class AccountDeletionRequest(
    val userId: String,
    val email: String,
    val role: String,
    val status: AccountDeletionStatus,
    val requestedAtMillis: Long = 0L
)

enum class AccountDeletionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    REJECTED;

    companion object {
        fun fromFirestore(value: String?): AccountDeletionStatus {
            return entries.firstOrNull { status ->
                status.name == value
            } ?: PENDING
        }
    }
}
