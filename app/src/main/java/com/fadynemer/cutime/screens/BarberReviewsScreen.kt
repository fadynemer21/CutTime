package com.fadynemer.cutime.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadynemer.cutime.R
import com.fadynemer.cutime.model.Rating
import com.fadynemer.cutime.ui.theme.CutTimeLightGrey
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.BarberReviewsViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberReviewsScreen(
    barberId: String,
    onBack: () -> Unit,
    canDeleteReviews: Boolean,
    reviewsViewModel: BarberReviewsViewModel = viewModel()
) {
    val uiState = reviewsViewModel.uiState
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var reviewToDelete by remember {
        mutableStateOf<Rating?>(null)
    }

    LaunchedEffect(barberId) {
        reviewsViewModel.observe(barberId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.barber_reviews_title),
                        color = CutTimeNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                stringResource(R.string.action_back),
                            tint = CutTimeNavy
                        )
                    }
                }
            )
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

            uiState.errorMessage != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(
                        onClick = reviewsViewModel::retry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CutTimeNavy
                        )
                    ) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }

            uiState.ratings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.barber_profile_no_reviews),
                        color = CutTimeTextSecondary
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = stringResource(
                                R.string.barber_reviews_count,
                                uiState.ratings.size
                            ),
                            color = CutTimeTextSecondary
                        )
                    }
                    uiState.actionErrorMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    items(uiState.ratings, key = { it.id }) { rating ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            RatingRow(rating)
                            if (
                                canDeleteReviews &&
                                rating.customerId == currentUserId
                            ) {
                                TextButton(
                                    onClick = { reviewToDelete = rating },
                                    enabled = uiState.deletingRatingId == null,
                                    modifier = Modifier.align(Alignment.End)
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.review_delete_action
                                        ),
                                        color = CutTimeRed
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(top = 16.dp),
                                color = CutTimeLightGrey
                            )
                        }
                    }
                }
            }
        }
    }

    reviewToDelete?.let { rating ->
        AlertDialog(
            onDismissRequest = { reviewToDelete = null },
            title = {
                Text(stringResource(R.string.review_delete_title))
            },
            text = {
                Text(stringResource(R.string.review_delete_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        reviewsViewModel.deleteReview(
                            rating.appointmentId
                        )
                        reviewToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeRed
                    )
                ) {
                    Text(stringResource(R.string.action_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { reviewToDelete = null }) {
                    Text(stringResource(R.string.action_no))
                }
            }
        )
    }
}
