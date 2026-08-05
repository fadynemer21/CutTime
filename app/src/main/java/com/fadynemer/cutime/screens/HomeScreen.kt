package com.fadynemer.cutime.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.fadynemer.cutime.R
import com.fadynemer.cutime.components.BarberCard
import com.fadynemer.cutime.components.CustomerBottomBar
import com.fadynemer.cutime.components.CustomerDestination
import com.fadynemer.cutime.components.NotificationIconButton
import com.fadynemer.cutime.model.CatalogSource
import com.fadynemer.cutime.navigation.AppRoute
import com.fadynemer.cutime.notifications.NotificationRegistrationManager
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeLightGrey
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.HomeViewModel
import com.fadynemer.cutime.util.AccountModePreferences
import com.fadynemer.cutime.viewmodel.NotificationBadgeViewModel
import com.fadynemer.cutime.viewmodel.SessionViewModel
import com.fadynemer.cutime.util.UiTestTags
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel(),
    sessionViewModel: SessionViewModel = viewModel(),
    notificationBadgeViewModel:
        NotificationBadgeViewModel = viewModel()
) {
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }
    val context = LocalContext.current

    val uiState = homeViewModel.uiState
    val barbers = uiState.barbers

    val filteredBarbers =
        remember(searchQuery, barbers) {
            if (searchQuery.isBlank()) {
                barbers
            } else {
                barbers.filter { barberShop ->
                    barberShop.name.contains(
                        other = searchQuery.trim(),
                        ignoreCase = true
                    )
                }
            }
        }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Ltr
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                CustomerBottomBar(
                    selectedDestination =
                        CustomerDestination.HOME,
                    onHomeSelected = {},
                    onAppointmentsSelected = {
                        navController.navigate(
                            AppRoute.CustomerAppointments.pattern
                        ) {
                            launchSingleTop = true
                        }
                    },
                    onProfileSelected = {
                        navController.navigate(
                            AppRoute.CustomerProfile.pattern
                        ) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = R.drawable.cutime_logo
                        ),
                        contentDescription = stringResource(
                            R.string.content_description_cutime_logo
                        ),
                        modifier = Modifier.size(150.dp)
                    )

                    Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                        NotificationIconButton(
                            unreadCount = notificationBadgeViewModel.uiState.customerCancellationUnreadCount,
                            onClick = {
                                navController.navigate(
                                    AppRoute.Notifications.create(false)
                                )
                            }
                        )
                        IconButton(onClick = {
                            FirebaseAuth.getInstance()
                                .currentUser?.uid
                                ?.let { userId ->
                                    AccountModePreferences.setCustomerMode(
                                        context, userId, false
                                    )
                                }
                            NotificationRegistrationManager.unregisterCurrentDevice {
                                sessionViewModel.logout()
                                navController.navigate(AppRoute.Welcome.pattern) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                stringResource(R.string.content_description_logout),
                                tint = CutTimeNavy
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.home_tagline),
                    fontSize = 14.sp,
                    color = CutTimeTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    placeholder = {
                        Text(stringResource(R.string.home_search_hint))
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = stringResource(
                                R.string.content_description_search
                            )
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(UiTestTags.HOME_SEARCH)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(
                            R.string.home_available_barbers
                        ),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CutTimeNavy,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() }
                    )

                    Text(
                        text = pluralStringResource(
                            R.plurals.home_available_count,
                            filteredBarbers.size,
                            filteredBarbers.size
                        ),
                        fontSize = 13.sp,
                        color = CutTimeTextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (uiState.source == CatalogSource.DEVELOPMENT_FALLBACK) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        color = CutTimeLightGrey
                    ) {
                        Text(
                            text = stringResource(
                                R.string.home_development_fallback
                            ),
                            color = CutTimeTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = CutTimeNavy)
                    }
                } else if (
                    uiState.errorMessage != null &&
                    barbers.isEmpty()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 50.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = homeViewModel::retry,
                            modifier = Modifier.testTag(
                                UiTestTags.HOME_RETRY
                            )
                        ) {
                            Text(stringResource(R.string.action_try_again))
                        }
                    }
                } else if (filteredBarbers.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(
                                R.string.home_no_barbers
                            ),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = CutTimeNavy
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = stringResource(
                                R.string.home_no_barbers_hint
                            ),
                            color = CutTimeTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(UiTestTags.HOME_CATALOG),
                        verticalArrangement =
                            Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(
                            bottom = 24.dp
                        )
                    ) {
                        uiState.errorMessage?.let { message ->
                            item {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape =
                                        MaterialTheme.shapes.medium,
                                    color = MaterialTheme
                                        .colorScheme.errorContainer
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        Text(
                                            text = stringResource(
                                                R.string.home_showing_saved
                                            ),
                                            color = MaterialTheme
                                                .colorScheme
                                                .onErrorContainer,
                                            fontWeight =
                                                FontWeight.SemiBold
                                        )
                                        Text(
                                            text = message,
                                            color = MaterialTheme
                                                .colorScheme
                                                .onErrorContainer,
                                            fontSize = 13.sp
                                        )
                                        TextButton(
                                            onClick =
                                                homeViewModel::retry,
                                            modifier = Modifier.align(
                                                Alignment.End
                                            )
                                        ) {
                                            Text(
                                                stringResource(
                                                    R.string.action_retry
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        items(
                            items = filteredBarbers,
                            key = { barberShop ->
                                barberShop.id
                            }
                        ) { barberShop ->
                            BarberCard(
                                barberShop = barberShop,
                                onViewProfile = {
                                    navController.navigate(
                                        AppRoute.BarberProfile.create(
                                            barberShop.id
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
