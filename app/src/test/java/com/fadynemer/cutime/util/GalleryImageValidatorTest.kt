package com.fadynemer.cutime.util

import com.fadynemer.cutime.model.GalleryLimits
import com.fadynemer.cutime.model.GalleryUploadProgress
import com.fadynemer.cutime.model.GalleryUploadRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GalleryImageValidatorTest {
    private fun request(
        uri: String = "content://image/one",
        type: String = "image/jpeg",
        size: Long = 500_000L,
        caption: String = "Clean fade"
    ) = GalleryUploadRequest(
        localUri = uri,
        contentType = type,
        sizeBytes = size,
        caption = caption
    )

    @Test
    fun validJpegPassesValidation() {
        assertNull(
            GalleryImageValidator.validate(
                request(),
                currentImageCount = 0
            )
        )
    }

    @Test
    fun everySupportedImageTypePassesValidation() {
        GalleryLimits.ALLOWED_CONTENT_TYPES.forEach { type ->
            assertNull(
                "$type should be allowed",
                GalleryImageValidator.validate(
                    request(type = type),
                    currentImageCount = 3
                )
            )
        }
    }

    @Test
    fun negativeGalleryCountIsRejected() {
        assertEquals(
            "The gallery state is invalid.",
            GalleryImageValidator.validate(request(), -1)
        )
    }

    @Test
    fun fullGalleryIsRejected() {
        assertEquals(
            "A gallery can contain up to 12 images.",
            GalleryImageValidator.validate(
                request(),
                GalleryLimits.MAX_IMAGES
            )
        )
    }

    @Test
    fun countImmediatelyBelowLimitIsAllowed() {
        assertNull(
            GalleryImageValidator.validate(
                request(),
                GalleryLimits.MAX_IMAGES - 1
            )
        )
    }

    @Test
    fun blankUriIsRejected() {
        assertEquals(
            "Choose an image to upload.",
            GalleryImageValidator.validate(
                request(uri = "  "),
                0
            )
        )
    }

    @Test
    fun arbitraryBinaryFileIsRejected() {
        assertEquals(
            "Choose a JPEG, PNG, WebP, HEIC, or HEIF image.",
            GalleryImageValidator.validate(
                request(type = "application/pdf"),
                0
            )
        )
    }

    @Test
    fun missingFileSizeIsRejected() {
        assertEquals(
            "The selected image is empty or unavailable.",
            GalleryImageValidator.validate(
                request(size = 0),
                0
            )
        )
    }

    @Test
    fun negativeFileSizeIsRejected() {
        assertEquals(
            "The selected image is empty or unavailable.",
            GalleryImageValidator.validate(
                request(size = -1),
                0
            )
        )
    }

    @Test
    fun eightMegabytesIsAllowed() {
        assertNull(
            GalleryImageValidator.validate(
                request(size = GalleryLimits.MAX_IMAGE_BYTES),
                0
            )
        )
    }

    @Test
    fun byteOverLimitIsRejected() {
        assertEquals(
            "Choose an image smaller than 8 MB.",
            GalleryImageValidator.validate(
                request(
                    size = GalleryLimits.MAX_IMAGE_BYTES + 1
                ),
                0
            )
        )
    }

    @Test
    fun captionAtLimitIsAllowed() {
        assertNull(
            GalleryImageValidator.validate(
                request(
                    caption = "a".repeat(
                        GalleryLimits.MAX_CAPTION_LENGTH
                    )
                ),
                0
            )
        )
    }

    @Test
    fun captionOverLimitIsRejected() {
        assertEquals(
            "Keep the caption under 120 characters.",
            GalleryImageValidator.validate(
                request(
                    caption = "a".repeat(
                        GalleryLimits.MAX_CAPTION_LENGTH + 1
                    )
                ),
                0
            )
        )
    }

    @Test
    fun captionSanitizerFlattensNewLinesAndTrims() {
        assertEquals(
            "First line  Second line",
            GalleryImageValidator.sanitizeCaption(
                "  First line\n Second line  "
            )
        )
    }

    @Test
    fun captionSanitizerCapsLength() {
        assertEquals(
            GalleryLimits.MAX_CAPTION_LENGTH,
            GalleryImageValidator.sanitizeCaption(
                "x".repeat(200)
            ).length
        )
    }

    @Test
    fun progressWithUnknownTotalIsZero() {
        val progress = GalleryUploadProgress(50, 0)
        assertEquals(0f, progress.fraction)
        assertEquals(0, progress.percent)
    }

    @Test
    fun progressCalculatesPercentage() {
        val progress = GalleryUploadProgress(75, 100)
        assertEquals(0.75f, progress.fraction)
        assertEquals(75, progress.percent)
    }

    @Test
    fun progressIsClampedAboveTotal() {
        val progress = GalleryUploadProgress(150, 100)
        assertEquals(1f, progress.fraction)
        assertEquals(100, progress.percent)
    }

    @Test
    fun supportedSetIncludesModernPhoneFormats() {
        assertTrue(
            GalleryLimits.ALLOWED_CONTENT_TYPES
                .containsAll(
                    listOf("image/heic", "image/heif", "image/webp")
                )
        )
    }
}
