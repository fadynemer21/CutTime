package com.fadynemer.cutime.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadynemer.cutime.R
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AppointmentDateTime
import com.fadynemer.cutime.viewmodel.BarberAppointmentHistoryUiState
import com.fadynemer.cutime.viewmodel.BarberAppointmentHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BarberAppointmentHistoryScreen(
    onBack: () -> Unit,
    onAppointmentSelected: (String) -> Unit,
    historyViewModel: BarberAppointmentHistoryViewModel = viewModel()
) {
    var appointmentToDelete by remember {
        mutableStateOf<Appointment?>(null)
    }
    var confirmClear by remember {
        mutableStateOf(false)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.barber_history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    if (!historyViewModel.uiState.isEmpty) {
                        TextButton(
                            onClick = { confirmClear = true },
                            enabled = !historyViewModel.uiState.isClearing
                        ) {
                            Text(stringResource(R.string.appointments_clear_history))
                        }
                    }
                }
            )
        }
    ) { padding ->
        BarberAppointmentHistoryContent(
            uiState = historyViewModel.uiState,
            onRetry = historyViewModel::retry,
            onAppointmentSelected = onAppointmentSelected,
            onDeleteFromHistory = { appointmentToDelete = it },
            modifier = Modifier.padding(padding)
        )
    }

    appointmentToDelete?.let { appointment ->
        AlertDialog(
            onDismissRequest = { appointmentToDelete = null },
            title = {
                Text(stringResource(R.string.barber_history_delete_title))
            },
            text = {
                Text(stringResource(R.string.barber_history_delete_message))
            },
            confirmButton = {
                Button(
                    onClick = {
                        historyViewModel.deleteFromHistory(appointment.id)
                        appointmentToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = CutTimeRed
                    )
                ) {
                    Text(stringResource(R.string.appointments_delete_action))
                }
            },
            dismissButton = {
                TextButton(onClick = { appointmentToDelete = null }) {
                    Text(stringResource(R.string.action_keep))
                }
            }
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = {
                Text(stringResource(R.string.appointments_clear_history_title))
            },
            text = {
                Text(stringResource(R.string.appointments_clear_history_message))
            },
            confirmButton = {
                Button(onClick = {
                    historyViewModel.clearHistory()
                    confirmClear = false
                }) {
                    Text(stringResource(R.string.appointments_clear_history))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text(stringResource(R.string.action_keep))
                }
            }
        )
    }
}

@Composable
private fun BarberAppointmentHistoryContent(
    uiState: BarberAppointmentHistoryUiState,
    onRetry: () -> Unit,
    onAppointmentSelected: (String) -> Unit,
    onDeleteFromHistory: (Appointment) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        uiState.isLoading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) { CircularProgressIndicator(color = CutTimeNavy) }

        uiState.errorMessage != null ||
            uiState.useGenericErrorMessage -> Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = uiState.errorMessage
                    ?: stringResource(R.string.barber_history_load_failed),
                color = CutTimeTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
        }

        uiState.isEmpty -> Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.barber_history_empty),
                color = CutTimeTextSecondary,
                textAlign = TextAlign.Center
            )
        }

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.appointments, key = Appointment::id) { appointment ->
                HistoryAppointmentCard(
                    appointment,
                    onAppointmentSelected,
                    onDeleteFromHistory
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryAppointmentCard(
    appointment: Appointment,
    onSelected: (String) -> Unit,
    onDelete: (Appointment) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { onSelected(appointment.id) },
                onLongClickLabel = stringResource(
                    R.string.appointments_delete_long_press
                ),
                onLongClick = { onDelete(appointment) }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = null, tint = CutTimeNavy)
                Spacer(Modifier.size(9.dp))
                Text(
                    text = appointment.customerName,
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.appointments_price, appointment.price),
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(appointment.serviceName, color = CutTimeTextSecondary)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.appointment_detail_status) + ": ",
                    color = CutTimeNavy,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (appointment.status) {
                        AppointmentStatus.COMPLETED ->
                            stringResource(R.string.appointments_completed)
                        AppointmentStatus.CANCELLED ->
                            stringResource(R.string.appointments_cancelled)
                        AppointmentStatus.UPCOMING -> ""
                    },
                    color = if (appointment.status == AppointmentStatus.COMPLETED) {
                        CutTimeSuccess
                    } else {
                        CutTimeRed
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(8.dp))
            HistoryMetadata(
                icon = Icons.Default.CalendarMonth,
                text = AppointmentDateTime.formatDateForDisplay(appointment.appointmentDate)
            )
            Spacer(Modifier.height(6.dp))
            HistoryMetadata(
                icon = Icons.Default.Schedule,
                text = pluralStringResource(
                    R.plurals.appointments_time_duration,
                    appointment.durationMinutes,
                    appointment.appointmentTime,
                    appointment.durationMinutes
                )
            )
        }
    }
}

@Composable
private fun HistoryMetadata(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = CutTimeNavy, modifier = Modifier.size(17.dp))
        Spacer(Modifier.size(6.dp))
        Text(text, color = CutTimeTextSecondary, fontSize = 13.sp)
    }
}
