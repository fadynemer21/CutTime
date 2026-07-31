package com.fadynemer.cutime.screens

import com.fadynemer.cutime.R

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.material3.TextButton
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
import com.fadynemer.cutime.components.NotificationIconButton
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.navigation.AppRoute
import com.fadynemer.cutime.notifications.NotificationRegistrationManager
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AppointmentDateTime
import com.fadynemer.cutime.viewmodel.BarberDashboardUiState
import com.fadynemer.cutime.viewmodel.BarberDashboardViewModel
import com.fadynemer.cutime.viewmodel.BarberShopReadinessUiState
import com.fadynemer.cutime.viewmodel.BarberShopReadinessViewModel
import com.fadynemer.cutime.viewmodel.SessionViewModel
import com.fadynemer.cutime.viewmodel.NotificationBadgeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel = viewModel(),
    dashboardViewModel: BarberDashboardViewModel = viewModel(),
    notificationBadgeViewModel:
        NotificationBadgeViewModel = viewModel(),
    shopReadinessViewModel:
        BarberShopReadinessViewModel = viewModel()
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
                            text = stringResource(R.string.dashboard_title),
                            color = CutTimeNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    actions = {
                        NotificationIconButton(
                            unreadCount =
                                notificationBadgeViewModel.uiState
                                    .unreadCount,
                            onClick = {
                                navController.navigate(
                                    AppRoute.Notifications.create(true)
                                )
                            }
                        )
                        IconButton(
                            onClick = {
                                NotificationRegistrationManager
                                    .unregisterCurrentDevice {
                                        sessionViewModel.logout()
                                        navController.navigate(
                                            AppRoute.Welcome.pattern
                                        ) {
                                            popUpTo(
                                                AppRoute.BarberDashboard
                                                    .pattern
                                            ) {
                                                inclusive = true
                                            }
                                            launchSingleTop = true
                                        }
                                    }
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.Logout,
                                contentDescription = stringResource(R.string.content_description_logout),
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
                readinessUiState =
                    shopReadinessViewModel.uiState,
                onRetry = dashboardViewModel::retry,
                onReadinessRetry =
                    shopReadinessViewModel::retry,
                onProfileSetup = {
                    navController.navigate(
                        AppRoute.BarberManageProfile.pattern
                    )
                },
                onServicesSetup = {
                    navController.navigate(
                        AppRoute.BarberServices.pattern
                    )
                },
                onAvailabilitySetup = {
                    navController.navigate(
                        AppRoute.BarberAvailability.pattern
                    )
                },
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
    readinessUiState: BarberShopReadinessUiState,
    onRetry: () -> Unit,
    onReadinessRetry: () -> Unit,
    onProfileSetup: () -> Unit,
    onServicesSetup: () -> Unit,
    onAvailabilitySetup: () -> Unit,
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
                    text = stringResource(R.string.appointments_load_failed),
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
                    Text(stringResource(R.string.action_try_again))
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
                    ShopReadinessCard(
                        uiState = readinessUiState,
                        onRetry = onReadinessRetry,
                        onProfileSetup = onProfileSetup,
                        onServicesSetup = onServicesSetup,
                        onAvailabilitySetup =
                            onAvailabilitySetup
                    )
                }

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
private fun ShopReadinessCard(
    uiState: BarberShopReadinessUiState,
    onRetry: () -> Unit,
    onProfileSetup: () -> Unit,
    onServicesSetup: () -> Unit,
    onAvailabilitySetup: () -> Unit
) {
    val readiness = uiState.readiness
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (readiness.isBookable) {
                    CutTimeSuccess.copy(alpha = 0.10f)
                } else {
                    CutTimeNavy.copy(alpha = 0.06f)
                }
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            when {
                uiState.isLoading -> {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = CutTimeNavy,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Text(
                            text = stringResource(R.string.dashboard_setup_checking),
                            color = CutTimeNavy,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                uiState.errorMessage != null -> {
                    Text(
                        text = stringResource(R.string.dashboard_setup_check_failed),
                        color = CutTimeNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = uiState.errorMessage,
                        color = CutTimeTextSecondary,
                        fontSize = 13.sp
                    )
                    TextButton(onClick = onRetry) {
                        Text(stringResource(R.string.action_try_again))
                    }
                }

                readiness.isBookable -> {
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector =
                                Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = CutTimeSuccess,
                            modifier = Modifier.size(30.dp)
                        )
                        Spacer(modifier = Modifier.size(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.dashboard_shop_live),
                                color = CutTimeNavy,
                                fontWeight =
                                    FontWeight.SemiBold,
                                fontSize = 18.sp
                            )
                            Text(
                                text =
                                    stringResource(R.string.dashboard_shop_live_hint),
                                color = CutTimeTextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                else -> {
                    Text(
                        text = stringResource(R.string.dashboard_finish_setup),
                        color = CutTimeNavy,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text =
                            "${readiness.completedStepCount} of ${readiness.totalStepCount} required steps complete",
                        color = CutTimeTextSecondary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    ReadinessStep(
                        label = "Public shop profile",
                        complete = readiness.profileComplete,
                        actionLabel = "Edit",
                        onAction = onProfileSetup
                    )
                    ReadinessStep(
                        label = "At least one service",
                        complete = readiness.servicesComplete,
                        actionLabel = "Services",
                        onAction = onServicesSetup
                    )
                    ReadinessStep(
                        label = "Saved hours with an open day",
                        complete =
                            readiness.availabilityComplete,
                        actionLabel = "Hours",
                        onAction = onAvailabilitySetup
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text =
                            stringResource(R.string.dashboard_finish_setup_hint),
                        color = CutTimeTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadinessStep(
    label: String,
    complete: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector =
                if (complete) {
                    Icons.Default.CheckCircle
                } else {
                    Icons.Default.RadioButtonUnchecked
                },
            contentDescription =
                if (complete) "Complete" else "Incomplete",
            tint =
                if (complete) {
                    CutTimeSuccess
                } else {
                    CutTimeTextSecondary
                },
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = label,
            color = CutTimeNavy,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp
        )
        TextButton(onClick = onAction) {
            Text(actionLabel)
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
                    text = stringResource(
                        R.string.appointments_price,
                        appointment.price
                    ),
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
            AppointmentMetadataRow(
                icon = Icons.Default.CalendarMonth,
                text = AppointmentDateTime.formatDateForDisplay(
                    appointment.appointmentDate
                )
            )
            Spacer(modifier = Modifier.height(6.dp))
            AppointmentMetadataRow(
                icon = Icons.Default.Schedule,
                text = pluralStringResource(
                    R.plurals.appointments_time_duration,
                    appointment.durationMinutes,
                    appointment.appointmentTime,
                    appointment.durationMinutes
                )
            )
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
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = {
                        onComplete(appointment.id)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeNavy
                    )
                ) {
                    Text(stringResource(R.string.action_complete))
                }
            }
        }
    }
}

@Composable
private fun AppointmentMetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CutTimeNavy,
            modifier = Modifier.size(17.dp)
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text = text,
            color = CutTimeTextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
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
