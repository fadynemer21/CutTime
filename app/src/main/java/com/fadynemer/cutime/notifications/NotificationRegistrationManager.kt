package com.fadynemer.cutime.notifications

import android.content.Context
import android.os.Build
import com.fadynemer.cutime.model.DeviceRegistration
import com.fadynemer.cutime.repository.NotificationDataSource
import com.fadynemer.cutime.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

object NotificationRegistrationManager {
    fun sync(
        context: Context,
        repository:
            NotificationDataSource = NotificationRepository(),
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            onResult(Result.success(Unit))
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                registerToken(
                    context = context,
                    token = token,
                    repository = repository,
                    onResult = onResult
                )
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun registerToken(
        context: Context,
        token: String,
        repository: NotificationDataSource,
        onResult: (Result<Unit>) -> Unit = {}
    ) {
        val packageInfo =
            context.packageManager.getPackageInfo(
                context.packageName,
                0
            )
        repository.registerDevice(
            DeviceRegistration(
                token = token,
                appVersion =
                    packageInfo.versionName ?: "unknown",
                deviceModel =
                    "${Build.MANUFACTURER} ${Build.MODEL}".trim()
            ),
            onResult
        )
    }

    /**
     * Removes this installation from the signed-in user's device registry
     * before authentication is cleared. Logout continues even when FCM cannot
     * return a token so a network problem can never trap someone in a session.
     */
    fun unregisterCurrentDevice(
        repository:
            NotificationDataSource = NotificationRepository(),
        onFinished: () -> Unit
    ) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            onFinished()
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                repository.unregisterDevice(token) {
                    onFinished()
                }
            }
            .addOnFailureListener {
                onFinished()
            }
    }
}
