package com.fadynemer.cutime.screens

import com.fadynemer.cutime.R

import androidx.compose.ui.res.stringResource

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.fadynemer.cutime.components.BarberDestination
import com.fadynemer.cutime.components.BarberManagementScaffold
import com.fadynemer.cutime.model.GalleryImage
import com.fadynemer.cutime.model.GalleryLimits
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.SelectedGalleryImageResolver
import com.fadynemer.cutime.viewmodel.BarberGalleryManagementViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberGalleryManagementScreen(
    navController: NavController,
    galleryViewModel:
        BarberGalleryManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = galleryViewModel.uiState
    val barberId =
        FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val imagePicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            if (uri != null) {
                SelectedGalleryImageResolver
                    .resolve(context, uri)
                    .onSuccess(galleryViewModel::upload)
                    .onFailure { error ->
                        galleryViewModel.reportError(
                            error.localizedMessage
                                ?: "The selected image could not be read."
                        )
                    }
            }
        }

    LaunchedEffect(barberId) {
        if (barberId.isNotBlank()) {
            galleryViewModel.observe(barberId)
        }
    }

    uiState.editingImageId?.let {
        CaptionDialog(
            value = uiState.captionDraft,
            isSaving = uiState.isSavingCaption,
            onValueChange = galleryViewModel::updateCaptionDraft,
            onSave = galleryViewModel::saveCaption,
            onDismiss = galleryViewModel::cancelCaptionEdit
        )
    }

    if (uiState.deletingImageId != null) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isDeleting) {
                    galleryViewModel.cancelDelete()
                }
            },
            title = { Text(stringResource(R.string.gallery_delete_title)) },
            text = {
                Text(stringResource(R.string.gallery_delete_message))
            },
            confirmButton = {
                Button(
                    onClick = galleryViewModel::confirmDelete,
                    enabled = !uiState.isDeleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeRed
                    )
                ) {
                    if (uiState.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.action_delete))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = galleryViewModel::cancelDelete,
                    enabled = !uiState.isDeleting
                ) {
                    Text(stringResource(R.string.action_keep))
                }
            }
        )
    }

    BarberManagementScaffold(
        title = "Gallery",
        selectedDestination = BarberDestination.PROFILE,
        onDestinationSelected = { destination ->
            navigateToBarberDestination(navController, destination)
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CutTimeNavy)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding =
                        androidx.compose.foundation.layout
                            .PaddingValues(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        GalleryManagementHeader(
                            imageCount = uiState.images.size,
                            canUpload = uiState.canUpload,
                            isUploading = uiState.isUploading,
                            uploadPercent = uiState.uploadPercent,
                            onChooseImage = {
                                imagePicker.launch("image/*")
                            }
                        )
                    }

                    uiState.errorMessage?.let { message ->
                        item {
                            MessageCard(
                                message = message,
                                isError = true
                            )
                        }
                    }
                    uiState.successMessage?.let { message ->
                        item {
                            MessageCard(
                                message = message,
                                isError = false
                            )
                        }
                    }

                    if (uiState.images.isEmpty()) {
                        item {
                            EmptyGalleryCard()
                        }
                    } else {
                        itemsIndexed(
                            items = uiState.images,
                            key = { _, image -> image.id }
                        ) { index, image ->
                            val actionsEnabled =
                                !uiState.isUploading &&
                                    !uiState.isSavingCaption &&
                                    !uiState.isDeleting &&
                                    !uiState.isReordering
                            ManagedGalleryImageCard(
                                image = image,
                                actionsEnabled = actionsEnabled,
                                canMoveEarlier =
                                    index > 0 &&
                                        actionsEnabled,
                                canMoveLater =
                                    index <
                                        uiState.images.lastIndex &&
                                        actionsEnabled,
                                onEditCaption = {
                                    galleryViewModel
                                        .beginCaptionEdit(image)
                                },
                                onDelete = {
                                    galleryViewModel
                                        .requestDelete(image.id)
                                },
                                onMoveEarlier = {
                                    galleryViewModel
                                        .moveEarlier(image.id)
                                },
                                onMoveLater = {
                                    galleryViewModel
                                        .moveLater(image.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryManagementHeader(
    imageCount: Int,
    canUpload: Boolean,
    isUploading: Boolean,
    uploadPercent: Int,
    onChooseImage: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.gallery_show_work),
            color = CutTimeNavy,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text =
                "$imageCount/${GalleryLimits.MAX_IMAGES} images. JPEG, PNG, WebP, HEIC, or HEIF up to 8 MB.",
            color = CutTimeTextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onChooseImage,
            enabled = canUpload,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CutTimeNavy
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AddPhotoAlternate,
                contentDescription = null
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                if (isUploading) {
                    "Uploading $uploadPercent%"
                } else {
                    "Choose Image"
                }
            )
        }
        if (isUploading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { uploadPercent / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = CutTimeNavy
            )
        }
    }
}

@Composable
private fun ManagedGalleryImageCard(
    image: GalleryImage,
    actionsEnabled: Boolean,
    canMoveEarlier: Boolean,
    canMoveLater: Boolean,
    onEditCaption: () -> Unit,
    onDelete: () -> Unit,
    onMoveEarlier: () -> Unit,
    onMoveLater: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            AsyncImage(
                model = image.downloadUrl,
                contentDescription =
                    image.caption.ifBlank {
                        "Barber gallery image"
                    },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 10f)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp
                        )
                    ),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text =
                        image.caption.ifBlank {
                            "No caption"
                        },
                    color =
                        if (image.caption.isBlank()) {
                            CutTimeTextSecondary
                        } else {
                            CutTimeNavy
                        },
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row {
                        IconButton(
                            onClick = onMoveEarlier,
                            enabled = canMoveEarlier
                        ) {
                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = stringResource(R.string.content_description_move_earlier)
                            )
                        }
                        IconButton(
                            onClick = onMoveLater,
                            enabled = canMoveLater
                        ) {
                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = stringResource(R.string.content_description_move_later)
                            )
                        }
                    }
                    Row {
                        IconButton(
                            onClick = onEditCaption,
                            enabled = actionsEnabled
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.content_description_edit_caption),
                                tint = CutTimeNavy
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            enabled = actionsEnabled
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = stringResource(R.string.content_description_delete_image),
                                tint = CutTimeRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyGalleryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Image,
                contentDescription = null,
                tint = CutTimeNavy,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.gallery_empty),
                color = CutTimeNavy,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text =
                    stringResource(R.string.gallery_empty_hint),
                color = CutTimeTextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CaptionDialog(
    value: String,
    isSaving: Boolean,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {
            if (!isSaving) onDismiss()
        },
        title = { Text(stringResource(R.string.gallery_caption_title)) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.gallery_caption_optional)) },
                minLines = 2,
                maxLines = 4,
                supportingText = {
                    Text(
                        stringResource(R.string.character_count, value.length, GalleryLimits.MAX_CAPTION_LENGTH)
                    )
                }
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun MessageCard(
    message: String,
    isError: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                if (isError) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    CutTimeSuccess.copy(alpha = 0.12f)
                }
        )
    ) {
        Text(
            text = message,
            color =
                if (isError) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    CutTimeSuccess
                },
            modifier = Modifier.padding(14.dp)
        )
    }
}
