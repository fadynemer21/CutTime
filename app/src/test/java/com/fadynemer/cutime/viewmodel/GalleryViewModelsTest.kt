package com.fadynemer.cutime.viewmodel

import com.fadynemer.cutime.model.GalleryLimits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarberGalleryViewModelTest {
    private val repository = FakeGalleryDataSource()
    private val viewModel = BarberGalleryViewModel(repository)

    @Test
    fun observeStartsLoadingRequestedBarber() {
        viewModel.observe("barber_1")

        assertEquals("barber_1", viewModel.uiState.barberId)
        assertTrue(viewModel.uiState.isLoading)
        assertEquals("barber_1", repository.observedBarberId)
    }

    @Test
    fun successfulObservationShowsImages() {
        viewModel.observe("barber_1")
        val images = listOf(
            testGalleryImage("first"),
            testGalleryImage("second", sortOrder = 1)
        )

        repository.emitGallery(Result.success(images))

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(images, viewModel.uiState.images)
        assertNull(viewModel.uiState.errorMessage)
    }

    @Test
    fun observationFailureShowsMessage() {
        viewModel.observe("barber_1")

        repository.emitGallery(
            Result.failure(IllegalStateException("Network offline"))
        )

        assertFalse(viewModel.uiState.isLoading)
        assertEquals(
            "Network offline",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun selectingKnownImageExposesSelectedImage() {
        viewModel.observe("barber_1")
        repository.emitGallery(
            Result.success(
                listOf(
                    testGalleryImage("first"),
                    testGalleryImage("second")
                )
            )
        )

        viewModel.selectImage("second")

        assertEquals("second", viewModel.uiState.selectedImageId)
        assertEquals("second", viewModel.uiState.selectedImage?.id)
    }

    @Test
    fun selectingUnknownImageDoesNothing() {
        viewModel.observe("barber_1")
        repository.emitGallery(
            Result.success(listOf(testGalleryImage("first")))
        )

        viewModel.selectImage("missing")

        assertNull(viewModel.uiState.selectedImageId)
    }

    @Test
    fun clearSelectionClosesImage() {
        viewModel.observe("barber_1")
        repository.emitGallery(
            Result.success(listOf(testGalleryImage("first")))
        )
        viewModel.selectImage("first")

        viewModel.clearSelection()

        assertNull(viewModel.uiState.selectedImageId)
        assertNull(viewModel.uiState.selectedImage)
    }

    @Test
    fun refreshedGalleryClearsDeletedSelection() {
        viewModel.observe("barber_1")
        repository.emitGallery(
            Result.success(listOf(testGalleryImage("first")))
        )
        viewModel.selectImage("first")

        repository.emitGallery(Result.success(emptyList()))

        assertNull(viewModel.uiState.selectedImageId)
    }

    @Test
    fun repeatedObserveForSameBarberDoesNotDuplicateListener() {
        viewModel.observe("barber_1")
        viewModel.observe("barber_1")

        assertEquals(1, repository.observationCount)
        assertEquals(0, repository.observationStopCount)
    }

    @Test
    fun changingBarberStopsPreviousListener() {
        viewModel.observe("barber_1")
        viewModel.observe("barber_2")

        assertEquals(2, repository.observationCount)
        assertEquals(1, repository.observationStopCount)
        assertEquals("barber_2", repository.observedBarberId)
    }

    @Test
    fun retryRestartsCurrentListener() {
        viewModel.observe("barber_1")
        repository.emitGallery(
            Result.failure(IllegalStateException("Failed"))
        )

        viewModel.retry()

        assertEquals(2, repository.observationCount)
        assertEquals(1, repository.observationStopCount)
        assertTrue(viewModel.uiState.isLoading)
    }
}

class BarberGalleryManagementViewModelTest {
    private val repository = FakeGalleryDataSource()
    private val viewModel =
        BarberGalleryManagementViewModel(repository)

    private fun load(
        images: List<com.fadynemer.cutime.model.GalleryImage> =
            listOf(testGalleryImage())
    ) {
        viewModel.observe("barber_1")
        repository.emitGallery(Result.success(images))
    }

    @Test
    fun blankBarberIdDoesNotStartObservation() {
        viewModel.observe(" ")

        assertEquals(0, repository.observationCount)
    }

    @Test
    fun loadedGalleryCanUploadBelowLimit() {
        load()

        assertFalse(viewModel.uiState.isLoading)
        assertTrue(viewModel.uiState.canUpload)
    }

    @Test
    fun fullGalleryCannotUpload() {
        load(
            List(GalleryLimits.MAX_IMAGES) { index ->
                testGalleryImage(
                    id = "image_$index",
                    sortOrder = index
                )
            }
        )

        assertFalse(viewModel.uiState.canUpload)
        viewModel.upload(testUploadRequest())
        assertNull(repository.uploadedRequest)
    }

    @Test
    fun validUploadEntersProgressState() {
        load(emptyList())
        val request = testUploadRequest()

        viewModel.upload(request)

        assertEquals(request, repository.uploadedRequest)
        assertTrue(viewModel.uiState.isUploading)
        assertEquals(0, viewModel.uiState.uploadPercent)
        assertFalse(viewModel.uiState.canUpload)
    }

    @Test
    fun uploadProgressUpdatesPercent() {
        load(emptyList())
        viewModel.upload(testUploadRequest())

        repository.emitUploadProgress(3, 4)

        assertEquals(75, viewModel.uiState.uploadPercent)
    }

    @Test
    fun uploadSuccessShowsConfirmation() {
        load(emptyList())
        viewModel.upload(testUploadRequest())

        repository.completeUpload(
            Result.success(testGalleryImage())
        )

        assertFalse(viewModel.uiState.isUploading)
        assertEquals(100, viewModel.uiState.uploadPercent)
        assertEquals(
            "Image added.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun uploadFailureResetsProgressAndShowsMessage() {
        load(emptyList())
        viewModel.upload(testUploadRequest())
        repository.emitUploadProgress(9, 10)

        repository.completeUpload(
            Result.failure(IllegalStateException("Upload failed"))
        )

        assertFalse(viewModel.uiState.isUploading)
        assertEquals(0, viewModel.uiState.uploadPercent)
        assertEquals(
            "Upload failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun invalidUploadNeverReachesRepository() {
        load(emptyList())

        viewModel.upload(
            testUploadRequest(contentType = "text/plain")
        )

        assertNull(repository.uploadedRequest)
        assertEquals(
            "Choose a JPEG, PNG, WebP, HEIC, or HEIF image.",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun captionEditStartsFromExistingCaption() {
        val image = testGalleryImage(caption = "Existing")
        load(listOf(image))

        viewModel.beginCaptionEdit(image)

        assertEquals(image.id, viewModel.uiState.editingImageId)
        assertEquals("Existing", viewModel.uiState.captionDraft)
    }

    @Test
    fun captionDraftAcceptsLimitAndRejectsOverLimit() {
        val image = testGalleryImage()
        load(listOf(image))
        viewModel.beginCaptionEdit(image)
        val allowed = "a".repeat(GalleryLimits.MAX_CAPTION_LENGTH)

        viewModel.updateCaptionDraft(allowed)
        viewModel.updateCaptionDraft("$allowed!")

        assertEquals(allowed, viewModel.uiState.captionDraft)
    }

    @Test
    fun saveCaptionUsesEditedImage() {
        val image = testGalleryImage()
        load(listOf(image))
        viewModel.beginCaptionEdit(image)
        viewModel.updateCaptionDraft("New caption")

        viewModel.saveCaption()

        assertEquals(image.id, repository.updatedCaptionImageId)
        assertEquals("New caption", repository.updatedCaption)
    }

    @Test
    fun captionSuccessClosesEditor() {
        val image = testGalleryImage()
        load(listOf(image))
        viewModel.beginCaptionEdit(image)
        viewModel.updateCaptionDraft("New caption")
        viewModel.saveCaption()

        repository.completeCaption(Result.success(Unit))

        assertNull(viewModel.uiState.editingImageId)
        assertEquals("", viewModel.uiState.captionDraft)
        assertEquals(
            "Caption updated.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun captionFailureKeepsEditorOpenForRetry() {
        val image = testGalleryImage()
        load(listOf(image))
        viewModel.beginCaptionEdit(image)
        viewModel.saveCaption()

        repository.completeCaption(
            Result.failure(IllegalStateException("Save failed"))
        )

        assertEquals(image.id, viewModel.uiState.editingImageId)
        assertEquals(
            "Save failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun deleteRequiresKnownImage() {
        load(listOf(testGalleryImage("known")))

        viewModel.requestDelete("missing")

        assertNull(viewModel.uiState.deletingImageId)
    }

    @Test
    fun confirmedDeleteUsesPendingImage() {
        load(listOf(testGalleryImage("known")))
        viewModel.requestDelete("known")

        viewModel.confirmDelete()

        assertEquals("known", repository.deletedImageId)
    }

    @Test
    fun deleteSuccessClearsConfirmation() {
        load(listOf(testGalleryImage("known")))
        viewModel.requestDelete("known")
        viewModel.confirmDelete()

        repository.completeDelete(Result.success(Unit))

        assertNull(viewModel.uiState.deletingImageId)
        assertEquals(
            "Image deleted.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun deleteFailureAlsoClosesConfirmation() {
        load(listOf(testGalleryImage("known")))
        viewModel.requestDelete("known")
        viewModel.confirmDelete()

        repository.completeDelete(
            Result.failure(IllegalStateException("Delete failed"))
        )

        assertNull(viewModel.uiState.deletingImageId)
        assertEquals(
            "Delete failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun movingLaterSendsCompleteNewOrder() {
        load(
            listOf(
                testGalleryImage("one", sortOrder = 0),
                testGalleryImage("two", sortOrder = 1),
                testGalleryImage("three", sortOrder = 2)
            )
        )

        viewModel.moveLater("one")

        assertEquals(
            listOf("two", "one", "three"),
            repository.reorderedImageIds
        )
        assertEquals(
            listOf("two", "one", "three"),
            viewModel.uiState.images.map { it.id }
        )
        assertTrue(viewModel.uiState.isReordering)
    }

    @Test
    fun movingEarlierUsesPreviousPosition() {
        load(
            listOf(
                testGalleryImage("one", sortOrder = 0),
                testGalleryImage("two", sortOrder = 1)
            )
        )

        viewModel.moveEarlier("two")

        assertEquals(
            listOf("two", "one"),
            repository.reorderedImageIds
        )
    }

    @Test
    fun boundaryMovesAreIgnored() {
        load(
            listOf(
                testGalleryImage("one", sortOrder = 0),
                testGalleryImage("two", sortOrder = 1)
            )
        )

        viewModel.moveEarlier("one")
        viewModel.moveLater("two")

        assertNull(repository.reorderedImageIds)
        assertFalse(viewModel.uiState.isReordering)
    }

    @Test
    fun reorderSuccessKeepsOptimisticOrder() {
        load(
            listOf(
                testGalleryImage("one", sortOrder = 0),
                testGalleryImage("two", sortOrder = 1)
            )
        )
        viewModel.moveLater("one")

        repository.completeReorder(Result.success(Unit))

        assertEquals(
            listOf("two", "one"),
            viewModel.uiState.images.map { it.id }
        )
        assertFalse(viewModel.uiState.isReordering)
        assertEquals(
            "Gallery order updated.",
            viewModel.uiState.successMessage
        )
    }

    @Test
    fun reorderFailureRollsBackOriginalOrder() {
        load(
            listOf(
                testGalleryImage("one", sortOrder = 0),
                testGalleryImage("two", sortOrder = 1)
            )
        )
        viewModel.moveLater("one")

        repository.completeReorder(
            Result.failure(IllegalStateException("Order failed"))
        )

        assertEquals(
            listOf("one", "two"),
            viewModel.uiState.images.map { it.id }
        )
        assertFalse(viewModel.uiState.isReordering)
        assertEquals(
            "Order failed",
            viewModel.uiState.errorMessage
        )
    }

    @Test
    fun dismissMessageClearsSuccessAndError() {
        load()
        viewModel.reportError("Problem")

        viewModel.dismissMessage()

        assertNull(viewModel.uiState.errorMessage)
        assertNull(viewModel.uiState.successMessage)
    }
}
