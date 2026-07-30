package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.DeviceRegistration
import com.fadynemer.cutime.model.GalleryImage
import com.fadynemer.cutime.model.GalleryUploadProgress
import com.fadynemer.cutime.model.GalleryUploadRequest
import com.fadynemer.cutime.model.NotificationPreferences
import com.fadynemer.cutime.model.UserNotification
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.GalleryDataSource
import com.fadynemer.cutime.repository.NotificationDataSource

internal fun testGalleryImage(
    id: String = "image_1",
    barberId: String = "barber_1",
    caption: String = "Fresh taper",
    sortOrder: Int = 0
) = GalleryImage(
    id = id,
    barberId = barberId,
    storagePath = "barberGalleries/$barberId/$id",
    downloadUrl = "https://example.test/$id.jpg",
    caption = caption,
    sortOrder = sortOrder,
    contentType = "image/jpeg",
    sizeBytes = 512_000L,
    createdAtMillis = 1_700_000_000_000L,
    updatedAtMillis = 1_700_000_100_000L
)

internal fun testUploadRequest(
    localUri: String = "content://gallery/image_1",
    contentType: String = "image/jpeg",
    sizeBytes: Long = 512_000L,
    caption: String = "Fresh taper"
) = GalleryUploadRequest(
    localUri = localUri,
    contentType = contentType,
    sizeBytes = sizeBytes,
    caption = caption
)

internal class FakeGalleryDataSource : GalleryDataSource {
    private var observationCallback:
        ((Result<List<GalleryImage>>) -> Unit)? = null
    private var uploadProgressCallback:
        ((GalleryUploadProgress) -> Unit)? = null
    private var uploadResultCallback:
        ((Result<GalleryImage>) -> Unit)? = null
    private var captionCallback:
        ((Result<Unit>) -> Unit)? = null
    private var deleteCallback:
        ((Result<Unit>) -> Unit)? = null
    private var reorderCallback:
        ((Result<Unit>) -> Unit)? = null

    var observedBarberId: String? = null
        private set
    var observationCount = 0
        private set
    var observationStopCount = 0
        private set
    var uploadedRequest: GalleryUploadRequest? = null
        private set
    var updatedCaptionImageId: String? = null
        private set
    var updatedCaption: String? = null
        private set
    var deletedImageId: String? = null
        private set
    var reorderedImageIds: List<String>? = null
        private set

    override fun observeGallery(
        barberId: String,
        onResult: (Result<List<GalleryImage>>) -> Unit
    ): AppointmentObservation {
        observedBarberId = barberId
        observationCount += 1
        observationCallback = onResult
        return AppointmentObservation {
            observationStopCount += 1
        }
    }

    override fun uploadImage(
        request: GalleryUploadRequest,
        onProgress: (GalleryUploadProgress) -> Unit,
        onResult: (Result<GalleryImage>) -> Unit
    ) {
        uploadedRequest = request
        uploadProgressCallback = onProgress
        uploadResultCallback = onResult
    }

    override fun updateCaption(
        imageId: String,
        caption: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        updatedCaptionImageId = imageId
        updatedCaption = caption
        captionCallback = onResult
    }

    override fun deleteImage(
        imageId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        deletedImageId = imageId
        deleteCallback = onResult
    }

    override fun reorderImages(
        imageIds: List<String>,
        onResult: (Result<Unit>) -> Unit
    ) {
        reorderedImageIds = imageIds
        reorderCallback = onResult
    }

    fun emitGallery(result: Result<List<GalleryImage>>) {
        observationCallback?.invoke(result)
    }

    fun emitUploadProgress(
        bytesTransferred: Long,
        totalBytes: Long
    ) {
        uploadProgressCallback?.invoke(
            GalleryUploadProgress(
                bytesTransferred = bytesTransferred,
                totalByteCount = totalBytes
            )
        )
    }

    fun completeUpload(result: Result<GalleryImage>) {
        uploadResultCallback?.invoke(result)
    }

    fun completeCaption(result: Result<Unit>) {
        captionCallback?.invoke(result)
    }

    fun completeDelete(result: Result<Unit>) {
        deleteCallback?.invoke(result)
    }

    fun completeReorder(result: Result<Unit>) {
        reorderCallback?.invoke(result)
    }
}

internal class FakeNotificationDataSource :
    NotificationDataSource {
    private var notificationsCallback:
        ((Result<List<UserNotification>>) -> Unit)? = null
    private var preferencesCallback:
        ((Result<NotificationPreferences>) -> Unit)? = null
    private var markReadCallback:
        ((Result<Unit>) -> Unit)? = null
    private var markAllCallback:
        ((Result<Unit>) -> Unit)? = null
    private var deleteCallback:
        ((Result<Unit>) -> Unit)? = null
    private var saveCallback:
        ((Result<Unit>) -> Unit)? = null
    private var registerCallback:
        ((Result<Unit>) -> Unit)? = null
    private var unregisterCallback:
        ((Result<Unit>) -> Unit)? = null

    var notificationsObservationCount = 0
        private set
    var notificationsStopCount = 0
        private set
    var preferencesObservationCount = 0
        private set
    var markedNotificationId: String? = null
        private set
    var markAllCalls = 0
        private set
    var deletedNotificationId: String? = null
        private set
    var savedPreferences: NotificationPreferences? = null
        private set
    var registeredDevice: DeviceRegistration? = null
        private set
    var unregisteredToken: String? = null
        private set

    override fun observeNotifications(
        onResult: (Result<List<UserNotification>>) -> Unit
    ): AppointmentObservation {
        notificationsObservationCount += 1
        notificationsCallback = onResult
        return AppointmentObservation {
            notificationsStopCount += 1
        }
    }

    override fun observePreferences(
        onResult: (Result<NotificationPreferences>) -> Unit
    ): AppointmentObservation {
        preferencesObservationCount += 1
        preferencesCallback = onResult
        return AppointmentObservation {}
    }

    override fun markRead(
        notificationId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        markedNotificationId = notificationId
        markReadCallback = onResult
    }

    override fun markAllRead(
        onResult: (Result<Unit>) -> Unit
    ) {
        markAllCalls += 1
        markAllCallback = onResult
    }

    override fun deleteNotification(
        notificationId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        deletedNotificationId = notificationId
        deleteCallback = onResult
    }

    override fun savePreferences(
        preferences: NotificationPreferences,
        onResult: (Result<Unit>) -> Unit
    ) {
        savedPreferences = preferences
        saveCallback = onResult
    }

    override fun registerDevice(
        registration: DeviceRegistration,
        onResult: (Result<Unit>) -> Unit
    ) {
        registeredDevice = registration
        registerCallback = onResult
    }

    override fun unregisterDevice(
        token: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        unregisteredToken = token
        unregisterCallback = onResult
    }

    fun emitNotifications(
        result: Result<List<UserNotification>>
    ) {
        notificationsCallback?.invoke(result)
    }

    fun emitPreferences(
        result: Result<NotificationPreferences>
    ) {
        preferencesCallback?.invoke(result)
    }

    fun completeMarkRead(result: Result<Unit>) {
        markReadCallback?.invoke(result)
    }

    fun completeMarkAll(result: Result<Unit>) {
        markAllCallback?.invoke(result)
    }

    fun completeDelete(result: Result<Unit>) {
        deleteCallback?.invoke(result)
    }

    fun completeSave(result: Result<Unit>) {
        saveCallback?.invoke(result)
    }

    fun completeRegister(result: Result<Unit>) {
        registerCallback?.invoke(result)
    }

    fun completeUnregister(result: Result<Unit>) {
        unregisterCallback?.invoke(result)
    }
}
