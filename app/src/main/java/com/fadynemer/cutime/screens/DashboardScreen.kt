package com.fadynemer.cutime.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fadynemer.cutime.components.BarberBottomBar
import com.fadynemer.cutime.components.BarberDestination
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.navigation.AppRoute
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AppointmentDateTime
import com.fadynemer.cutime.viewmodel.BarberDashboardUiState
import com.fadynemer.cutime.viewmodel.BarberDashboardViewModel
import com.fadynemer.cutime.viewmodel.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel = viewModel(),
    dashboardViewModel: BarberDashboardViewModel = viewModel()
) {
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
                            text = "Barber Dashboard",
                            color = CutTimeNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                sessionViewModel.logout()
                                navController.navigate(
                                    AppRoute.Welcome.pattern
                                ) {
                                    popUpTo(
                                        AppRoute.BarberDashboard.pattern
                                    ) {
                                        inclusive = true
                                    }
                                    launchSingleTop = true
                                }
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Logout",
                                tint = CutTimeNavy
                            )
                        }
                    }
                )
            },
            bottomBar = {
                BarberBottomBar(
                    selectedDestination =
                        BarberDestination.DASHBOARD,
                    onDestinationSelected = { destination ->
                        navigateToBarberDestination(
                            navController,
                            destination
                        )
                    }
                )
            }
        ) { innerPadding ->
            DashboardContent(
                uiState = dashboardViewModel.uiState,
                onRetry = dashboardViewModel::retry,
                onComplete =
                    dashboardViewModel::completeAppointment,
                onCancel =
                    dashboardViewModel::cancelAppointment,
                onAppointmentSelected = { appointmentId ->
                    navController.navigate(
                        AppRoute.BarberAppointmentDetail.create(
                            appointmentId
                        )
                    )
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: BarberDashboardUiState,
    onRetry: () -> Unit,
    onComplete: (String) -> Unit,
    onCancel: (String) -> Unit,
    onAppointmentSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CutTimeNavy)
            }
        }

        uiState.errorMessage != null -> {
            Column(
                modifier = modifier.padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Could not load appointments",
                    color = CutTimeNavy,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.errorMessage,
                    color = CutTimeTextSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeNavy
                    )
                ) {
                    Text("Try Again")
                }
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier,
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    DashboardSummary(
                        todayCount = uiState.today.size,
                        upcomingCount = uiState.upcoming.size
                    )
                }

                item {
                    DashboardSectionTitle("Today")
                }

                if (uiState.today.isEmpty()) {
                    item {
                        DashboardEmptyCard(
                            "No appointments scheduled for today."
                        )
                    }
                } else {
                    items(
                        count = uiState.today.size,
                        key = { index -> uiState.today[index].id }
                    ) { index ->
                        BarberAppointmentCard(
                            appointment = uiState.today[index],
                            onComplete = onComplete,
                            onCancel = onCancel,
                            onSelected = onAppointmentSelected
                        )
                    }
                }

                item {
                    DashboardSectionTitle("Upcoming")
                }

                if (uiState.upcoming.isEmpty()) {
                    item {
                        DashboardEmptyCard(
                            "No upcoming appointments yet."
                        )
                    }
                } else {
                    items(
                        count = uiState.upcoming.size,
                        key = { index ->
                            uiState.upcoming[index].id
                        }
                    ) { index ->
                        BarberAppointmentCard(
                            appointment = uiState.upcoming[index],
                            onComplete = onComplete,
                            onCancel = onCancel,
                            onSelected = onAppointmentSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardSummary(
    todayCount: Int,
    upcomingCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SummaryMetric(
            label = "Today",
            value = todayCount,
            modifier = Modifier.weight(1f)
        )
        SummaryMetric(
            label = "Upcoming",
            value = upcomingCount,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryMetric(
    label: String,
    value: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = value.toString(),
                color = CutTimeNavy,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                color = CutTimeTextSecondary
            )
        }
    }
}

@Composable
private fun BarberAppointmentCard(
    appointment: Appointment,
    onComplete: (String) -> Unit,
    onCancel: (String) -> Unit,
    onSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelected(appointment.id)
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = CutTimeNavy
                )
                Spacer(modifier = Modifier.size(9.dp))
                Text(
                    text = appointment.customerName,
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "₪${appointment.price}",
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = appointment.serviceName,
                color = CutTimeTextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = CutTimeNavy,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text =
                        AppointmentDateTime.formatDateForDisplay(
                            appointment.appointmentDate
                        ),
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.size(14.dp))
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = CutTimeNavy,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                Text(
                    text =
                        "${appointment.appointmentTime} • " +
                            "${appointment.durationMinutes} min",
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onCancel(appointment.id)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        onComplete(appointment.id)
                    },
                    modifier = Modifier.weight(1f),
                    enabled =
                        appointment.endAtMillis <=
                            System.currentTimeMillis(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeNavy
                    )
                ) {
                    Text("Complete")
                }
            }
        }
    }
}

@Composable
private fun DashboardSectionTitle(
    title: String
) {
    Text(
        text = title,
        color = CutTimeNavy,
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun DashboardEmptyCard(
    message: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Text(
            text = message,
            color = CutTimeTextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp)
        )
    }
}

fun navigateToBarberDestination(
    navController: NavController,
    destination: BarberDestination
) {
    navController.navigate(destination.route) {
        popUpTo(AppRoute.BarberDashboard.pattern) {
            inclusive = false
        }
        launchSingleTop = true
    }
}
