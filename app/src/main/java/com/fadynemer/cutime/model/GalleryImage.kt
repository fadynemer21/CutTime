package com.fadynemer.cutime.model

data class GalleryImage(
    val id: String,
    val barberId: String,
    val storagePath: String,
    val downloadUrl: String,
    val caption: String,
    val sortOrder: Int,
    val contentType: String,
    val sizeBytes: Long,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

data class GalleryUploadRequest(
    val localUri: String,
    val contentType: String,
    val sizeBytes: Long,
    val caption: String = ""
)

data class GalleryUploadProgress(
    val bytesTransferred: Long,
    val totalByteCount: Long
) {
    val fraction: Float
        get() =
            if (totalByteCount <= 0L) {
                0f
            } else {
                (
                    bytesTransferred.toDouble() /
                        totalByteCount.toDouble()
                    )
                    .coerceIn(0.0, 1.0)
                    .toFloat()
            }

    val percent: Int
        get() = (fraction * 100f).toInt()
}

object GalleryLimits {
    const val MAX_IMAGES = 12
    const val MAX_IMAGE_BYTES = 8L * 1024L * 1024L
    const val MAX_CAPTION_LENGTH = 120

    val ALLOWED_CONTENT_TYPES = setOf(
        "image/jpeg",
        "image/png",
        "image/webp",
        "image/heic",
        "image/heif"
    )
}
