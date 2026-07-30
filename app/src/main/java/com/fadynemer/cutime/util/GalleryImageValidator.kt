package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.GalleryLimits
import com.fadynemer.cutime.model.GalleryUploadRequest

object GalleryImageValidator {
    fun validate(
        request: GalleryUploadRequest,
        currentImageCount: Int
    ): String? {
        return when {
            currentImageCount < 0 ->
                "The gallery state is invalid."

            currentImageCount >= GalleryLimits.MAX_IMAGES ->
                "A gallery can contain up to ${GalleryLimits.MAX_IMAGES} images."

            request.localUri.isBlank() ->
                "Choose an image to upload."

            request.contentType !in
                GalleryLimits.ALLOWED_CONTENT_TYPES ->
                "Choose a JPEG, PNG, WebP, HEIC, or HEIF image."

            request.sizeBytes <= 0L ->
                "The selected image is empty or unavailable."

            request.sizeBytes > GalleryLimits.MAX_IMAGE_BYTES ->
                "Choose an image smaller than 8 MB."

            request.caption.trim().length >
                GalleryLimits.MAX_CAPTION_LENGTH ->
                "Keep the caption under ${GalleryLimits.MAX_CAPTION_LENGTH} characters."

            else -> null
        }
    }

    fun sanitizeCaption(value: String): String {
        return value
            .replace('\n', ' ')
            .replace('\r', ' ')
            .trim()
            .take(GalleryLimits.MAX_CAPTION_LENGTH)
    }
}
