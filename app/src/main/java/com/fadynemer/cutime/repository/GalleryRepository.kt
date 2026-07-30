package com.fadynemer.cutime.repository

import android.net.Uri
import com.fadynemer.cutime.model.GalleryImage
import com.fadynemer.cutime.model.GalleryUploadProgress
import com.fadynemer.cutime.model.GalleryUploadRequest
import com.fadynemer.cutime.util.GalleryImageValidator
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata

interface GalleryDataSource {
    fun observeGallery(
        barberId: String,
        onResult: (Result<List<GalleryImage>>) -> Unit
    ): AppointmentObservation

    fun uploadImage(
        request: GalleryUploadRequest,
        onProgress: (GalleryUploadProgress) -> Unit,
        onResult: (Result<GalleryImage>) -> Unit
    )

    fun updateCaption(
        imageId: String,
        caption: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun deleteImage(
        imageId: String,
        onResult: (Result<Unit>) -> Unit
    )

    fun reorderImages(
        imageIds: List<String>,
        onResult: (Result<Unit>) -> Unit
    )
}

class GalleryAuthenticationException :
    Exception("Please sign in as a barber to manage the gallery.")

class GalleryRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage =
        FirebaseStorage.getInstance()
) : GalleryDataSource {

    override fun observeGallery(
        barberId: String,
        onResult: (Result<List<GalleryImage>>) -> Unit
    ): AppointmentObservation {
        if (barberId.isBlank()) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "The barber gallery is invalid."
                    )
                )
            )
            return AppointmentObservation {}
        }

        val registration =
            galleryCollection(barberId)
                .orderBy("sortOrder", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onResult(Result.failure(error))
                    } else {
                        val images =
                            snapshot
                                ?.documents
                                ?.mapNotNull(::mapImage)
                                .orEmpty()
                        onResult(Result.success(images))
                    }
                }

        return AppointmentObservation(registration::remove)
    }

    override fun uploadImage(
        request: GalleryUploadRequest,
        onProgress: (GalleryUploadProgress) -> Unit,
        onResult: (Result<GalleryImage>) -> Unit
    ) {
        val barberId = auth.currentUser?.uid

        if (barberId == null) {
            onResult(
                Result.failure(GalleryAuthenticationException())
            )
            return
        }

        galleryCollection(barberId)
            .get()
            .addOnSuccessListener { gallerySnapshot ->
                val validationError =
                    GalleryImageValidator.validate(
                        request = request,
                        currentImageCount =
                            gallerySnapshot.size()
                    )

                if (validationError != null) {
                    onResult(
                        Result.failure(
                            IllegalArgumentException(validationError)
                        )
                    )
                    return@addOnSuccessListener
                }

                startUpload(
                    barberId = barberId,
                    request = request,
                    sortOrder = gallerySnapshot.size(),
                    onProgress = onProgress,
                    onResult = onResult
                )
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun startUpload(
        barberId: String,
        request: GalleryUploadRequest,
        sortOrder: Int,
        onProgress: (GalleryUploadProgress) -> Unit,
        onResult: (Result<GalleryImage>) -> Unit
    ) {
        val imageReference =
            galleryCollection(barberId).document()
        val storagePath =
            "$STORAGE_ROOT/$barberId/${imageReference.id}"
        val storageReference =
            storage.reference.child(storagePath)
        val metadata =
            StorageMetadata.Builder()
                .setContentType(request.contentType)
                .setCustomMetadata("barberId", barberId)
                .setCustomMetadata(
                    "imageId",
                    imageReference.id
                )
                .build()
        val uploadTask =
            storageReference.putFile(
                Uri.parse(request.localUri),
                metadata
            )

        uploadTask.addOnProgressListener { snapshot ->
            onProgress(
                GalleryUploadProgress(
                    bytesTransferred = snapshot.bytesTransferred,
                    totalByteCount = snapshot.totalByteCount
                )
            )
        }.addOnFailureListener { error ->
            onResult(Result.failure(error))
        }.addOnSuccessListener {
            storageReference.downloadUrl
                .addOnSuccessListener { downloadUri ->
                    saveGalleryDocument(
                        barberId = barberId,
                        imageId = imageReference.id,
                        storagePath = storagePath,
                        downloadUrl = downloadUri.toString(),
                        request = request,
                        sortOrder = sortOrder,
                        onResult = onResult
                    )
                }
                .addOnFailureListener { error ->
                    deleteUploadedObjectBestEffort(
                        storagePath
                    )
                    onResult(Result.failure(error))
                }
        }
    }

    private fun saveGalleryDocument(
        barberId: String,
        imageId: String,
        storagePath: String,
        downloadUrl: String,
        request: GalleryUploadRequest,
        sortOrder: Int,
        onResult: (Result<GalleryImage>) -> Unit
    ) {
        val cleanCaption =
            GalleryImageValidator.sanitizeCaption(
                request.caption
            )
        val imageReference =
            galleryCollection(barberId).document(imageId)
        val profileReference =
            firestore.collection(PROFILES_COLLECTION)
                .document(barberId)
        val batch = firestore.batch()

        batch.set(
            imageReference,
            hashMapOf(
                "imageId" to imageId,
                "barberId" to barberId,
                "storagePath" to storagePath,
                "downloadUrl" to downloadUrl,
                "caption" to cleanCaption,
                "sortOrder" to sortOrder,
                "contentType" to request.contentType,
                "sizeBytes" to request.sizeBytes,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.update(
            profileReference,
            "updatedAt",
            FieldValue.serverTimestamp()
        )

        batch.commit()
            .addOnSuccessListener {
                onResult(
                    Result.success(
                        GalleryImage(
                            id = imageId,
                            barberId = barberId,
                            storagePath = storagePath,
                            downloadUrl = downloadUrl,
                            caption = cleanCaption,
                            sortOrder = sortOrder,
                            contentType = request.contentType,
                            sizeBytes = request.sizeBytes,
                            createdAtMillis = 0L,
                            updatedAtMillis = 0L
                        )
                    )
                )
            }
            .addOnFailureListener { error ->
                deleteUploadedObjectBestEffort(storagePath)
                onResult(Result.failure(error))
            }
    }

    override fun updateCaption(
        imageId: String,
        caption: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val barberId = requireBarberId(onResult) ?: return
        val cleanCaption =
            GalleryImageValidator.sanitizeCaption(caption)

        if (caption.trim().length > 120) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "Keep the caption under 120 characters."
                    )
                )
            )
            return
        }

        galleryCollection(barberId)
            .document(imageId)
            .update(
                mapOf(
                    "caption" to cleanCaption,
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

    override fun deleteImage(
        imageId: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        val barberId = requireBarberId(onResult) ?: return
        val imageReference =
            galleryCollection(barberId).document(imageId)

        imageReference.get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onResult(Result.success(Unit))
                    return@addOnSuccessListener
                }

                val storagePath =
                    document.getString("storagePath")
                        ?: "$STORAGE_ROOT/$barberId/$imageId"
                storage.reference.child(storagePath)
                    .delete()
                    .addOnSuccessListener {
                        removeGalleryDocument(
                            barberId,
                            imageReference,
                            onResult
                        )
                    }
                    .addOnFailureListener { error ->
                        if (
                            error is StorageException &&
                            error.errorCode ==
                            StorageException.ERROR_OBJECT_NOT_FOUND
                        ) {
                            removeGalleryDocument(
                                barberId,
                                imageReference,
                                onResult
                            )
                        } else {
                            onResult(Result.failure(error))
                        }
                    }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun removeGalleryDocument(
        barberId: String,
        imageReference:
            com.google.firebase.firestore.DocumentReference,
        onResult: (Result<Unit>) -> Unit
    ) {
        val batch = firestore.batch()
        batch.delete(imageReference)
        batch.update(
            firestore.collection(PROFILES_COLLECTION)
                .document(barberId),
            "updatedAt",
            FieldValue.serverTimestamp()
        )
        batch.commit()
            .addOnSuccessListener {
                compactSortOrder(barberId)
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    override fun reorderImages(
        imageIds: List<String>,
        onResult: (Result<Unit>) -> Unit
    ) {
        val barberId = requireBarberId(onResult) ?: return

        if (
            imageIds.isEmpty() ||
            imageIds.distinct().size != imageIds.size
        ) {
            onResult(
                Result.failure(
                    IllegalArgumentException(
                        "The gallery order is invalid."
                    )
                )
            )
            return
        }

        val batch = firestore.batch()
        imageIds.forEachIndexed { index, imageId ->
            batch.update(
                galleryCollection(barberId).document(imageId),
                mapOf(
                    "sortOrder" to index,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        batch.update(
            firestore.collection(PROFILES_COLLECTION)
                .document(barberId),
            "updatedAt",
            FieldValue.serverTimestamp()
        )
        batch.commit()
            .addOnSuccessListener {
                onResult(Result.success(Unit))
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    private fun compactSortOrder(barberId: String) {
        galleryCollection(barberId)
            .orderBy("sortOrder", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) return@addOnSuccessListener

                val batch = firestore.batch()
                snapshot.documents.forEachIndexed {
                        index,
                        document ->
                    if (
                        document.getLong("sortOrder")?.toInt() !=
                        index
                    ) {
                        batch.update(
                            document.reference,
                            "sortOrder",
                            index
                        )
                    }
                }
                batch.commit()
            }
    }

    private fun deleteUploadedObjectBestEffort(
        storagePath: String
    ) {
        storage.reference.child(storagePath).delete()
    }

    private fun <T> requireBarberId(
        onResult: (Result<T>) -> Unit
    ): String? {
        val barberId = auth.currentUser?.uid
        if (barberId == null) {
            onResult(
                Result.failure(GalleryAuthenticationException())
            )
        }
        return barberId
    }

    private fun galleryCollection(barberId: String) =
        firestore
            .collection(PROFILES_COLLECTION)
            .document(barberId)
            .collection(GALLERY_COLLECTION)

    private fun mapImage(
        document: DocumentSnapshot
    ): GalleryImage? {
        return GalleryImage(
            id =
                document.getString("imageId")
                    ?: document.id,
            barberId =
                document.getString("barberId")
                    ?: return null,
            storagePath =
                document.getString("storagePath")
                    ?: return null,
            downloadUrl =
                document.getString("downloadUrl")
                    ?: return null,
            caption =
                document.getString("caption").orEmpty(),
            sortOrder =
                document.getLong("sortOrder")?.toInt() ?: 0,
            contentType =
                document.getString("contentType")
                    ?: "image/jpeg",
            sizeBytes =
                document.getLong("sizeBytes") ?: 0L,
            createdAtMillis =
                document.getTimestamp("createdAt")
                    ?.toDate()?.time ?: 0L,
            updatedAtMillis =
                document.getTimestamp("updatedAt")
                    ?.toDate()?.time ?: 0L
        )
    }

    private companion object {
        const val PROFILES_COLLECTION = "barberProfiles"
        const val GALLERY_COLLECTION = "gallery"
        const val STORAGE_ROOT = "barberGalleries"
    }
}
