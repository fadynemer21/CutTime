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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadynemer.cutime.components.CustomerBottomBar
import com.fadynemer.cutime.components.CustomerDestination
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.ui.theme.CutTimeLightGrey
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AppointmentDateTime
import com.fadynemer.cutime.viewmodel.AppointmentsUiState
import com.fadynemer.cutime.viewmodel.AppointmentsViewModel

private enum class AppointmentSection(
    val title: String
) {
    UPCOMING("Upcoming"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentsScreen(
    onHomeSelected: () -> Unit,
    onBrowseBarbers: () -> Unit,
    onProfileSelected: () -> Unit,
    onAppointmentSelected: (String) -> Unit,
    appointmentsViewModel: AppointmentsViewModel = viewModel()
) {
    val uiState = appointmentsViewModel.uiState
    var appointmentToCancel by remember {
        mutableStateOf<Appointment?>(null)
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
                            text = "My Appointments",
                            color = CutTimeNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            },
            bottomBar = {
                CustomerBottomBar(
                    selectedDestination =
                        CustomerDestination.APPOINTMENTS,
                    onHomeSelected = onHomeSelected,
                    onAppointmentsSelected = {},
                    onProfileSelected = onProfileSelected
                )
            }
        ) { innerPadding ->
            AppointmentsContent(
                uiState = uiState,
                onRetry = appointmentsViewModel::retry,
                onBrowseBarbers = onBrowseBarbers,
                onCancel = { appointment ->
                    appointmentToCancel = appointment
                },
                onAppointmentSelected = onAppointmentSelected,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }
    }

    appointmentToCancel?.let { appointment ->
        AlertDialog(
            onDismissRequest = {
                appointmentToCancel = null
            },
            title = { Text("Cancel appointment?") },
            text = {
                Text(
                    "This will release the reserved time with ${appointment.barberName}."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        appointmentsViewModel.cancelAppointment(
                            appointment.id
                        )
                        appointmentToCancel = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeRed
                    )
                ) {
                    Text("Cancel Appointment")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        appointmentToCancel = null
                    }
                ) {
                    Text("Keep")
                }
            }
        )
    }
}

@Composable
private fun AppointmentsContent(
    uiState: AppointmentsUiState,
    onRetry: () -> Unit,
    onBrowseBarbers: () -> Unit,
    onCancel: (Appointment) -> Unit,
    onAppointmentSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = CutTimeNavy
                )
            }
        }

        uiState.errorMessage != null -> {
            AppointmentErrorState(
                message = uiState.errorMessage,
                onRetry = onRetry,
                modifier = modifier
            )
        }

        uiState.isEmpty -> {
            EmptyAppointmentsState(
                onBrowseBarbers = onBrowseBarbers,
                modifier = modifier
            )
        }

        else -> {
            LazyColumn(
                modifier = modifier.navigationBarsPadding(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 10.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.groups.upcoming.isNotEmpty()) {
                    item {
                        AppointmentSectionHeader(
                            section = AppointmentSection.UPCOMING,
                            count = uiState.groups.upcoming.size
                        )
                    }
                    items(
                        count = uiState.groups.upcoming.size,
                        key = { index ->
                            uiState.groups.upcoming[index].id
                        }
                    ) { index ->
                        AppointmentCard(
                            appointment =
                                uiState.groups.upcoming[index],
                            onCancel = onCancel,
                            onSelected = onAppointmentSelected
                        )
                    }
                }

                if (uiState.groups.completed.isNotEmpty()) {
                    item {
                        AppointmentSectionHeader(
                            section = AppointmentSection.COMPLETED,
                            count = uiState.groups.completed.size
                        )
                    }
                    items(
                        count = uiState.groups.completed.size,
                        key = { index ->
                            uiState.groups.completed[index].id
                        }
                    ) { index ->
                        AppointmentCard(
                            appointment =
                                uiState.groups.completed[index],
                            onCancel = null,
                            onSelected = onAppointmentSelected
                        )
                    }
                }

                if (uiState.groups.cancelled.isNotEmpty()) {
                    item {
                        AppointmentSectionHeader(
                            section = AppointmentSection.CANCELLED,
                            count = uiState.groups.cancelled.size
                        )
                    }
                    items(
                        count = uiState.groups.cancelled.size,
                        key = { index ->
                            uiState.groups.cancelled[index].id
                        }
                    ) { index ->
                        AppointmentCard(
                            appointment =
                                uiState.groups.cancelled[index],
                            onCancel = null,
                            onSelected = onAppointmentSelected
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentSectionHeader(
    section: AppointmentSection,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = section.title,
            color = CutTimeNavy,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            color = CutTimeTextSecondary,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun AppointmentCard(
    appointment: Appointment,
    onCancel: ((Appointment) -> Unit)?,
    onSelected: (String) -> Unit
) {
    val isCancelled =
        appointment.status == AppointmentStatus.CANCELLED
    val isCompleted =
        appointment.status == AppointmentStatus.COMPLETED ||
            appointment.endAtMillis <= System.currentTimeMillis()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelected(appointment.id)
            },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = CutTimeLightGrey
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ContentCut,
                            contentDescription = null,
                            tint = CutTimeNavy,
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.size(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appointment.barberName,
                        color = CutTimeNavy,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = appointment.serviceName,
                        color = CutTimeTextSecondary,
                        fontSize = 14.sp
                    )
                }

                AppointmentStatusBadge(
                    text =
                        when {
                            isCancelled -> "Cancelled"
                            isCompleted -> "Completed"
                            else -> "Upcoming"
                        },
                    isCancelled = isCancelled,
                    isCompleted = isCompleted
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = CutTimeNavy,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(7.dp))
                Text(
                    text =
                        AppointmentDateTime.formatDateForDisplay(
                            appointment.appointmentDate
                        ),
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
                Spacer(modifier = Modifier.size(7.dp))
                Text(
                    text =
                        "${appointment.appointmentTime} • " +
                            "${appointment.durationMinutes} minutes",
                    color = CutTimeTextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "₪${appointment.price}",
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (onCancel != null) {
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        onCancel(appointment)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Appointment")
                }
            }
        }
    }
}

@Composable
private fun AppointmentStatusBadge(
    text: String,
    isCancelled: Boolean,
    isCompleted: Boolean
) {
    val color =
        when {
            isCancelled -> CutTimeRed
            isCompleted -> CutTimeTextSecondary
            else -> CutTimeSuccess
        }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                horizontal = 9.dp,
                vertical = 6.dp
            )
        )
    }
}

@Composable
private fun EmptyAppointmentsState(
    onBrowseBarbers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = CutTimeNavy,
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(15.dp))
        Text(
            text = "No appointments yet",
            color = CutTimeNavy,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = "Book a barber and your appointments will appear here.",
            color = CutTimeTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onBrowseBarbers,
            colors = ButtonDefaults.buttonColors(
                containerColor = CutTimeNavy
            )
        ) {
            Text("Browse Barbers")
        }
    }
}

@Composable
private fun AppointmentErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = null,
            tint = CutTimeRed,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Could not load appointments",
            color = CutTimeNavy,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = CutTimeTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
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
