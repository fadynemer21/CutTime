package com.fadynemer.cutime.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.fadynemer.cutime.model.GalleryImage
import com.fadynemer.cutime.model.GalleryLimits
import com.fadynemer.cutime.model.GalleryUploadRequest
import com.fadynemer.cutime.repository.AppointmentObservation
import com.fadynemer.cutime.repository.GalleryDataSource
import com.fadynemer.cutime.repository.GalleryRepository
import com.fadynemer.cutime.util.GalleryImageValidator

data class PublicGalleryUiState(
    val barberId: String? = null,
    val isLoading: Boolean = false,
    val images: List<GalleryImage> = emptyList(),
    val selectedImageId: String? = null,
    val errorMessage: String? = null
) {
    val selectedImage: GalleryImage?
        get() = images.firstOrNull { it.id == selectedImageId }
}

class BarberGalleryViewModel(
    private val repository: GalleryDataSource = GalleryRepository()
) : ViewModel() {
    var uiState by mutableStateOf(PublicGalleryUiState())
        private set

    private var observation: AppointmentObservation? = null

    fun observe(barberId: String) {
        if (
            uiState.barberId == barberId &&
            observation != null
        ) {
            return
        }

        observation?.stop()
        uiState = PublicGalleryUiState(
            barberId = barberId,
            isLoading = true
        )
        observation = repository.observeGallery(
            barberId
        ) { result ->
            result
                .onSuccess { images ->
                    uiState = uiState.copy(
                        isLoading = false,
                        images = images,
                        selectedImageId =
                            uiState.selectedImageId
                                ?.takeIf { selectedId ->
                                    images.any {
                                        it.id == selectedId
                                    }
                                },
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Gallery could not be loaded."
                    )
                }
        }
    }

    fun selectImage(imageId: String) {
        if (uiState.images.any { it.id == imageId }) {
            uiState = uiState.copy(
                selectedImageId = imageId
            )
        }
    }

    fun clearSelection() {
        uiState = uiState.copy(selectedImageId = null)
    }

    fun retry() {
        val barberId = uiState.barberId ?: return
        observation?.stop()
        observation = null
        observe(barberId)
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}

data class GalleryManagementUiState(
    val barberId: String? = null,
    val isLoading: Boolean = true,
    val images: List<GalleryImage> = emptyList(),
    val isUploading: Boolean = false,
    val uploadPercent: Int = 0,
    val editingImageId: String? = null,
    val captionDraft: String = "",
    val isSavingCaption: Boolean = false,
    val deletingImageId: String? = null,
    val isDeleting: Boolean = false,
    val isReordering: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) {
    val canUpload: Boolean
        get() =
            !isLoading &&
                !isUploading &&
                !isSavingCaption &&
                !isDeleting &&
                !isReordering &&
                images.size < GalleryLimits.MAX_IMAGES
}

class BarberGalleryManagementViewModel(
    private val repository: GalleryDataSource = GalleryRepository()
) : ViewModel() {
    var uiState by mutableStateOf(GalleryManagementUiState())
        private set

    private var observation: AppointmentObservation? = null

    fun observe(barberId: String) {
        if (
            barberId.isBlank() ||
            (
                uiState.barberId == barberId &&
                    observation != null
                )
        ) {
            return
        }

        observation?.stop()
        uiState = GalleryManagementUiState(
            barberId = barberId
        )
        observation = repository.observeGallery(
            barberId
        ) { result ->
            result
                .onSuccess { images ->
                    uiState = uiState.copy(
                        isLoading = false,
                        images = images,
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isLoading = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Gallery could not be loaded."
                    )
                }
        }
    }

    fun upload(request: GalleryUploadRequest) {
        if (!uiState.canUpload) {
            return
        }

        val validationError =
            GalleryImageValidator.validate(
                request,
                uiState.images.size
            )
        if (validationError != null) {
            uiState = uiState.copy(
                errorMessage = validationError,
                successMessage = null
            )
            return
        }

        uiState = uiState.copy(
            isUploading = true,
            uploadPercent = 0,
            errorMessage = null,
            successMessage = null
        )
        repository.uploadImage(
            request = request,
            onProgress = { progress ->
                uiState = uiState.copy(
                    uploadPercent = progress.percent
                )
            },
            onResult = { result ->
                result
                    .onSuccess {
                        uiState = uiState.copy(
                            isUploading = false,
                            uploadPercent = 100,
                            successMessage = "Image added."
                        )
                    }
                    .onFailure { error ->
                        uiState = uiState.copy(
                            isUploading = false,
                            uploadPercent = 0,
                            errorMessage =
                                error.localizedMessage
                                    ?: "Image could not be uploaded."
                        )
                    }
            }
        )
    }

    fun beginCaptionEdit(image: GalleryImage) {
        if (
            uiState.isUploading ||
            uiState.isSavingCaption ||
            uiState.isDeleting ||
            uiState.isReordering
        ) return

        uiState = uiState.copy(
            editingImageId = image.id,
            captionDraft = image.caption,
            errorMessage = null,
            successMessage = null
        )
    }

    fun updateCaptionDraft(value: String) {
        if (value.length <= GalleryLimits.MAX_CAPTION_LENGTH) {
            uiState = uiState.copy(
                captionDraft = value,
                errorMessage = null
            )
        }
    }

    fun cancelCaptionEdit() {
        if (uiState.isSavingCaption) return

        uiState = uiState.copy(
            editingImageId = null,
            captionDraft = ""
        )
    }

    fun saveCaption() {
        val imageId = uiState.editingImageId ?: return
        if (uiState.isSavingCaption) return

        uiState = uiState.copy(
            isSavingCaption = true,
            errorMessage = null,
            successMessage = null
        )
        repository.updateCaption(
            imageId = imageId,
            caption = uiState.captionDraft
        ) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        editingImageId = null,
                        captionDraft = "",
                        isSavingCaption = false,
                        successMessage = "Caption updated.",
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        isSavingCaption = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Caption could not be updated."
                    )
                }
        }
    }

    fun requestDelete(imageId: String) {
        if (
            !uiState.isUploading &&
            !uiState.isSavingCaption &&
            !uiState.isDeleting &&
            !uiState.isReordering &&
            uiState.images.any { it.id == imageId }
        ) {
            uiState = uiState.copy(
                deletingImageId = imageId,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun cancelDelete() {
        if (uiState.isDeleting) return
        uiState = uiState.copy(deletingImageId = null)
    }

    fun confirmDelete() {
        val imageId = uiState.deletingImageId ?: return
        if (uiState.isDeleting) return

        uiState = uiState.copy(
            isDeleting = true,
            errorMessage = null,
            successMessage = null
        )
        repository.deleteImage(imageId) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        deletingImageId = null,
                        isDeleting = false,
                        successMessage = "Image deleted.",
                        errorMessage = null
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        deletingImageId = null,
                        isDeleting = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Image could not be deleted."
                    )
                }
        }
    }

    fun moveEarlier(imageId: String) {
        move(imageId, -1)
    }

    fun moveLater(imageId: String) {
        move(imageId, 1)
    }

    private fun move(
        imageId: String,
        offset: Int
    ) {
        if (
            uiState.isUploading ||
            uiState.isSavingCaption ||
            uiState.isDeleting ||
            uiState.isReordering
        ) return

        val currentIndex =
            uiState.images.indexOfFirst { it.id == imageId }
        val destinationIndex = currentIndex + offset

        if (
            currentIndex < 0 ||
            destinationIndex !in uiState.images.indices
        ) {
            return
        }

        val previousOrder = uiState.images
        val reordered = previousOrder.toMutableList()
        val image = reordered.removeAt(currentIndex)
        reordered.add(destinationIndex, image)
        uiState = uiState.copy(
            images = reordered,
            isReordering = true,
            errorMessage = null,
            successMessage = null
        )
        repository.reorderImages(
            reordered.map(GalleryImage::id)
        ) { result ->
            result
                .onSuccess {
                    uiState = uiState.copy(
                        images = reordered,
                        isReordering = false,
                        successMessage = "Gallery order updated."
                    )
                }
                .onFailure { error ->
                    uiState = uiState.copy(
                        images = previousOrder,
                        isReordering = false,
                        errorMessage =
                            error.localizedMessage
                                ?: "Gallery order could not be saved."
                    )
                }
        }
    }

    fun dismissMessage() {
        uiState = uiState.copy(
            errorMessage = null,
            successMessage = null
        )
    }

    fun reportError(message: String) {
        uiState = uiState.copy(
            errorMessage = message,
            successMessage = null
        )
    }

    override fun onCleared() {
        observation?.stop()
        super.onCleared()
    }
}
