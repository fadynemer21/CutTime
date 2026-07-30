package com.fadynemer.cutime.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.model.GalleryImage
import com.fadynemer.cutime.model.Rating
import com.fadynemer.cutime.ui.theme.CutTimeLightGrey
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.BarberReviewsViewModel
import com.fadynemer.cutime.viewmodel.BarberGalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberProfileScreen(
    barberShop: BarberShop?,
    onBack: () -> Unit,
    onBookAppointment: () -> Unit,
    reviewsViewModel: BarberReviewsViewModel = viewModel(),
    galleryViewModel: BarberGalleryViewModel = viewModel()
) {
    LaunchedEffect(barberShop?.id) {
        barberShop?.id?.let { barberId ->
            reviewsViewModel.observe(barberId)
            galleryViewModel.observe(barberId)
        }
    }

    galleryViewModel.uiState.selectedImage?.let { image ->
        GalleryImageDialog(
            image = image,
            onDismiss = galleryViewModel::clearSelection
        )
    }
    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Barber Profile",
                            color = CutTimeNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CutTimeNavy
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (barberShop == null) {
                BarberNotFound(
                    onBack = onBack,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            } else {
                BarberProfileContent(
                    barberShop = barberShop,
                    reviewsState = reviewsViewModel.uiState,
                    galleryState = galleryViewModel.uiState,
                    onRetryReviews = reviewsViewModel::retry,
                    onRetryGallery = galleryViewModel::retry,
                    onSelectGalleryImage =
                        galleryViewModel::selectImage,
                    onBookAppointment = onBookAppointment,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun BarberProfileContent(
    barberShop: BarberShop,
    reviewsState:
        com.fadynemer.cutime.viewmodel.BarberReviewsUiState,
    galleryState:
        com.fadynemer.cutime.viewmodel.PublicGalleryUiState,
    onRetryReviews: () -> Unit,
    onRetryGallery: () -> Unit,
    onSelectGalleryImage: (String) -> Unit,
    onBookAppointment: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ProfileHeader(barberShop = barberShop)
        }

        item {
            ProfileSection(title = "About") {
                Text(
                    text = barberShop.description,
                    color = CutTimeTextSecondary,
                    lineHeight = 22.sp
                )
            }
        }

        item {
            ProfileSection(title = "Services") {
                barberShop.services.forEachIndexed { index, service ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = service.name,
                                color = CutTimeNavy,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${service.durationMinutes} min",
                                color = CutTimeTextSecondary,
                                fontSize = 13.sp
                            )
                        }

                        Text(
                            text = "₪${service.price}",
                            color = CutTimeNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (index < barberShop.services.lastIndex) {
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }

        item {
            ProfileSection(title = "Opening Hours") {
                barberShop.openingHours.forEachIndexed { index, openingHours ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = openingHours.day,
                            color = CutTimeTextSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = openingHours.hours,
                            color = CutTimeNavy,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (index < barberShop.openingHours.lastIndex) {
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }
        }

        item {
            ProfileSection(title = "Customer Reviews") {
                ReviewsContent(
                    ratings = reviewsState.reviewsWithText,
                    isLoading = reviewsState.isLoading,
                    errorMessage = reviewsState.errorMessage,
                    onRetry = onRetryReviews
                )
            }
        }

        item {
            CustomerGallerySection(
                images = galleryState.images,
                isLoading = galleryState.isLoading,
                errorMessage = galleryState.errorMessage,
                developmentPlaceholderCount =
                    if (barberShop.isDevelopmentFallback) {
                        barberShop.galleryItemCount
                    } else {
                        0
                    },
                onRetry = onRetryGallery,
                onSelectImage = onSelectGalleryImage
            )
        }

        item {
            Column {
                SectionTitle("Available Times")
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = barberShop.nextAvailable.substringBefore(" at"),
                    color = CutTimeTextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(barberShop.availableTimes) { time ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CutTimeLightGrey
                        ) {
                            Text(
                                text = time,
                                color = CutTimeNavy,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(
                                    horizontal = 18.dp,
                                    vertical = 11.dp
                                )
                            )
                        }
                    }
                }
            }
        }

        item {
            if (barberShop.isDevelopmentFallback) {
                Text(
                    text =
                        "Development preview only. Booking requires a real Barber account with a saved shop profile, service, and availability.",
                    color = CutTimeRed,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Button(
                onClick = onBookAppointment,
                enabled = !barberShop.isDevelopmentFallback,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                Text(
                    text =
                        if (barberShop.isDevelopmentFallback) {
                            "Booking Disabled for Preview Data"
                        } else {
                            "Book Appointment"
                        },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ReviewsContent(
    ratings: List<Rating>,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CutTimeNavy,
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
            }
        }

        errorMessage != null -> {
            Column(
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeNavy
                    )
                ) {
                    Text("Retry")
                }
            }
        }

        ratings.isEmpty() -> {
            Text(
                text = "No written reviews yet.",
                color = CutTimeTextSecondary
            )
        }

        else -> {
            ratings.take(5).forEachIndexed { index, rating ->
                RatingRow(rating)
                if (index < ratings.take(5).lastIndex) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomerGallerySection(
    images: List<GalleryImage>,
    isLoading: Boolean,
    errorMessage: String?,
    developmentPlaceholderCount: Int,
    onRetry: () -> Unit,
    onSelectImage: (String) -> Unit
) {
    Column {
        SectionTitle("Gallery")
        Spacer(modifier = Modifier.height(10.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(116.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = CutTimeNavy,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                }
            }

            images.isNotEmpty() -> {
                LazyRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    items(images, key = GalleryImage::id) { image ->
                        AsyncImage(
                            model = image.downloadUrl,
                            contentDescription =
                                image.caption.ifBlank {
                                    "Barber gallery image"
                                },
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(116.dp)
                                .clickable {
                                    onSelectImage(image.id)
                                }
                        )
                    }
                }
            }

            developmentPlaceholderCount > 0 -> {
                LazyRow(
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    items(developmentPlaceholderCount) {
                        Surface(
                            modifier = Modifier.size(116.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = CutTimeLightGrey
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription =
                                        "Development gallery placeholder",
                                    tint = CutTimeNavy,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }
                    }
                }
            }

            errorMessage != null -> {
                Column {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CutTimeNavy
                        )
                    ) {
                        Text("Retry")
                    }
                }
            }

            else -> {
                Text(
                    text = "No gallery images yet.",
                    color = CutTimeTextSecondary
                )
            }
        }
    }
}

@Composable
private fun GalleryImageDialog(
    image: GalleryImage,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text =
                    image.caption.ifBlank {
                        "Gallery image"
                    }
            )
        },
        text = {
            AsyncImage(
                model = image.downloadUrl,
                contentDescription =
                    image.caption.ifBlank {
                        "Barber gallery image"
                    },
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun RatingRow(rating: Rating) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = rating.customerName,
                color = CutTimeNavy,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Row {
                repeat(5) { index ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint =
                            if (index < rating.stars) {
                                CutTimeRed
                            } else {
                                CutTimeLightGrey
                            },
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = rating.review,
            color = CutTimeTextSecondary,
            lineHeight = 20.sp,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun ProfileHeader(
    barberShop: BarberShop
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(92.dp),
                shape = RoundedCornerShape(18.dp),
                color = CutTimeLightGrey
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ContentCut,
                        contentDescription = null,
                        tint = CutTimeNavy,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = barberShop.name,
                    color = CutTimeNavy,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = CutTimeRed,
                        modifier = Modifier.size(19.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text =
                            "${barberShop.rating} (${barberShop.reviewCount} reviews)",
                        color = CutTimeTextSecondary,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(7.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = CutTimeNavy,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = barberShop.nextAvailable,
                        color = CutTimeTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        SectionTitle(title)
        Spacer(modifier = Modifier.height(10.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        color = CutTimeNavy,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun BarberNotFound(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Barber not found",
            color = CutTimeNavy,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This barber profile is no longer available.",
            color = CutTimeTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = CutTimeNavy
            )
        ) {
            Text("Back to Home")
        }
    }
}
