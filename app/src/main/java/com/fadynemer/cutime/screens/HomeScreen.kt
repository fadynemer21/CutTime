package com.fadynemer.cutime.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Logout
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fadynemer.cutime.R
import com.fadynemer.cutime.components.BarberCard
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.SessionViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel = viewModel()
) {
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    val sampleBarbers = remember {
        listOf(
            BarberShop(
                id = "barber_1",
                name = "Urban Fade Studio",
                rating = 4.9,
                reviewCount = 128,
                startingPrice = 60,
                nextAvailable = "Today at 14:30"
            ),
            BarberShop(
                id = "barber_2",
                name = "Classic Cuts",
                rating = 4.8,
                reviewCount = 94,
                startingPrice = 50,
                nextAvailable = "Today at 16:00"
            ),
            BarberShop(
                id = "barber_3",
                name = "Sharp Style Barbers",
                rating = 4.7,
                reviewCount = 76,
                startingPrice = 55,
                nextAvailable = "Tomorrow at 10:30"
            ),
            BarberShop(
                id = "barber_4",
                name = "The Barber Room",
                rating = 4.6,
                reviewCount = 51,
                startingPrice = 45,
                nextAvailable = "Tomorrow at 12:00"
            )
        )
    }

    val filteredBarbers =
        remember(searchQuery, sampleBarbers) {
            if (searchQuery.isBlank()) {
                sampleBarbers
            } else {
                sampleBarbers.filter { barberShop ->
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
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
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

                    IconButton(
                        onClick = {
                            sessionViewModel.logout()

                            navController.navigate("welcome") {
                                popUpTo("home") {
                                    inclusive = true
                                }

                                launchSingleTop = true
                            }
                        },
                        modifier = Modifier.align(
                            Alignment.TopEnd
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = CutTimeNavy
                        )
                    }
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

                if (filteredBarbers.isEmpty()) {
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
                                    /*
                                     * Barber profile navigation
                                     * will be added next.
                                     */
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}