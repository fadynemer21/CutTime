package com.fadynemer.cutime.repository

import com.fadynemer.cutime.model.DeviceRegistration
import com.fadynemer.cutime.model.NotificationPreferences
import com.fadynemer.cutime.model.NotificationType
import com.fadynemer.cutime.model.UserNotification
import com.fadynemer.cutime.util.DeviceTokenHasher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

interface NotificationDataSource {
    fun observeNotifications(
        onResult: (Result<List<UserNotification>>) -> Unit
    ): AppointmentObservation?

    fun observePreferences(
        onResult: (Result<NotificationPreferences>) -> Unit
    ): AppointmentObservation?

    fun markRead(
        notificationId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun markAllRead(
        onResult: (Result<Unit>) -> Unit
    )

    fun deleteNotification(
        notificationId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun savePreferences(
        preferences: NotificationPreferences,
        onResult: (Result<Unit>) -> Unit
    )

    fun registerDevice(
        registration: DeviceRegistration,
        onResult: (Result<Unit>) -> Unit
    )

    fun unregisterDevice(
        token: String,
        onResult: (Result<Unit>) -> Unit
    )
}

class NotificationAuthenticationException :
    Exception("Please log in again to manage notifications.")

class NotificationRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance()
) : NotificationDataSource {

    override fun observeNotifications(
        onResult: (Result<List<UserNotification>>) -> Unit
    ): AppointmentObservation? {
        val userId = requireUserId(onResult) ?: return null
        val registration =
            notificationsCollection(userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(MAX_NOTIFICATIONS.toLong())
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        onResult(
                            Result.success(
                                snapshot
                                    ?.documents
                                    ?.mapNotNull(::mapNotification)
                                    .orEmpty()
                            )
                        )
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun observePreferences(
        onResult: (Result<NotificationPreferences>) -> Unit
    ): AppointmentObservation? {
        val userId = requireUserId(onResult) ?: return null
        val registration =
            settingsDocument(userId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        onResult(
                            Result.success(
                                snapshot
                                    ?.takeIf(DocumentSnapshot::exists)
                                    ?.let(::mapPreferences)
                                    ?: NotificationPreferences()
                            )
                        )
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun markRead(
        notificationId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = requireUserId(onResult) ?: return

        if (notificationId.isBlank()) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "The notification is invalid."
                    )
                )
            )
            return
        }

        notificationsCollection(userId)
            .document(notificationId)
            .update(
                mapOf(
                    "isRead" to true,
                    "readAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun markAllRead(
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = requireUserId(onResult) ?: return

        notificationsCollection(userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    onResult(Result.success(Unit))
                    return@addOnSuccessListener
                }

                val batch = firestore.batch()
                snapshot.documents.forEach { document ->
                    batch.update(
                        document.reference,
                        mapOf(
                            "isRead" to true,
                            "readAt" to
                                FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit()
                    .addOnSuccessListener {
                        onResult(Result.success(Unit))
                    }
                    .addOnFailureListener { error ->
                        onResult(Result.failure(error))
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun deleteNotification(
        notificationId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = requireUserId(onResult) ?: return
        notificationsCollection(userId)
            .document(notificationId)
            .delete()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun savePreferences(
        preferences: NotificationPreferences,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = requireUserId(onResult) ?: return
        settingsDocument(userId)
            .set(
                hashMapOf(
                    "userId" to userId,
                    "pushEnabled" to preferences.pushEnabled,
                    "remindersEnabled" to
                        preferences.remindersEnabled,
                    "appointmentUpdatesEnabled" to
                        preferences.appointmentUpdatesEnabled,
                    "reviewPromptsEnabled" to
                        preferences.reviewPromptsEnabled,
                    "reminderLeadMinutes" to
                        listOf(120, 30),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun registerDevice(
        registration: DeviceRegistration,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = requireUserId(onResult) ?: return

        if (
            registration.token.isBlank() ||
            registration.platform != "ANDROID" ||
            registration.appVersion.isBlank()
        ) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "The device registration is invalid."
                    )
                )
            )
            return
        }

        val documentId =
            DeviceTokenHasher.documentId(registration.token)
        devicesCollection(userId)
            .document(documentId)
            .set(
                hashMapOf(
                    "deviceId" to documentId,
                    "userId" to userId,
                    "token" to registration.token,
                    "platform" to registration.platform,
                    "appVersion" to registration.appVersion,
                    "deviceModel" to registration.deviceModel,
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

    override fun unregisterDevice(
        token: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val userId = requireUserId(onResult) ?: return

        if (token.isBlank()) {
            onResult(Result.success(Unit))
            return
        }

        devicesCollection(userId)
            .document(DeviceTokenHasher.documentId(token))
            .delete()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun mapNotification(
        document: DocumentSnapshot
    ): UserNotification? {
        return UserNotification(
            id =
                document.getString("notificationId")
                    ?: document.id,
            userId =
                document.getString("userId")
                    ?: return null,
            type =
                NotificationType.fromFirestore(
                    document.getString("type")
                ),
            title =
                document.getString("title")
                    ?: "CuTime",
            message =
                document.getString("message").orEmpty(),
            appointmentId =
                document.getString("appointmentId"),
            barberId = document.getString("barberId"),
            isRead =
                document.getBoolean("isRead") ?: false,
            createdAtMillis =
                document.getTimestamp("createdAt")
                    ?.toDate()?.time ?: 0L
        )
    }

    private fun mapPreferences(
        document: DocumentSnapshot
    ): NotificationPreferences {
        return NotificationPreferences(
            pushEnabled =
                document.getBoolean("pushEnabled") ?: true,
            remindersEnabled =
                document.getBoolean("remindersEnabled") ?: true,
            appointmentUpdatesEnabled =
                document.getBoolean(
                    "appointmentUpdatesEnabled"
                ) ?: true,
            reviewPromptsEnabled =
                document.getBoolean("reviewPromptsEnabled")
                    ?: true
        )
    }

    private fun notificationsCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(NOTIFICATIONS_COLLECTION)

    private fun devicesCollection(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(DEVICES_COLLECTION)

    private fun settingsDocument(userId: String) =
        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(SETTINGS_COLLECTION)
            .document(NOTIFICATION_SETTINGS_DOCUMENT)

    private fun <T> requireUserId(
        onResult: (Result<T>) -> Unit
    ): String? {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            onResult(
                Result.failure(
                    NotificationAuthenticationException()
                )
            )
        }
        return userId
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val NOTIFICATIONS_COLLECTION = "notifications"
        const val DEVICES_COLLECTION = "devices"
        const val SETTINGS_COLLECTION = "settings"
        const val NOTIFICATION_SETTINGS_DOCUMENT = "notifications"
        const val MAX_NOTIFICATIONS = 100
    }
}
