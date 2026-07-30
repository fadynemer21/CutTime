package com.fadynemer.cutime.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
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
import com.fadynemer.cutime.model.CatalogSource
import com.fadynemer.cutime.navigation.AppRoute
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeLightGrey
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.HomeViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun HomeScreen(
    navController: NavController,
    homeViewModel: HomeViewModel = viewModel()
) {
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

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
                        contentDescription = "CutTime Logo",
                        modifier = Modifier.size(150.dp)
                    )

                }

                Text(
                    text = "Find the right barber for your next cut.",
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
                        Text("Search barbers...")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Barbers",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CutTimeNavy,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "${filteredBarbers.size} available",
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
                            text =
                                "Development data is shown until real barber profiles are available.",
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
                } else if (uiState.errorMessage != null) {
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
                        Button(onClick = homeViewModel::retry) {
                            Text("Try Again")
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
                            text = "No barbers found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = CutTimeNavy
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Try searching for a different name.",
                            color = CutTimeTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement =
                            Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(
                            bottom = 24.dp
                        )
                    ) {
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
