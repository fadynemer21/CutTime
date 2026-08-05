package com.fadynemer.cutime.screens

import com.fadynemer.cutime.R

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
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
import com.fadynemer.cutime.model.Appointment
import com.fadynemer.cutime.model.AppointmentStatus
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AppointmentDateTime
import com.fadynemer.cutime.viewmodel.AppointmentDetailViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentDetailScreen(
    appointmentId: String,
    isBarberView: Boolean,
    canRate: Boolean = true,
    onBack: () -> Unit,
    onReschedule: (String) -> Unit,
    onRate: (String) -> Unit,
    detailViewModel: AppointmentDetailViewModel = viewModel()
) {
    val uiState = detailViewModel.uiState
    var confirmCancel by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(appointmentId) {
        detailViewModel.observe(appointmentId)
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
                            text = stringResource(
                                R.string.appointment_details_title
                            ),
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

                uiState.appointment == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text =
                                uiState.errorMessage
                                    ?: "Appointment unavailable.",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                else -> {
                    AppointmentDetailContent(
                        appointment = uiState.appointment,
                        isBarberView = isBarberView,
                        canRate = canRate,
                        isUpdating = uiState.isUpdating,
                        errorMessage = uiState.errorMessage,
                        successMessage = uiState.successMessage,
                        onCancel = {
                            confirmCancel = true
                        },
                        onComplete = detailViewModel::complete,
                        onReschedule = {
                            onReschedule(uiState.appointment.id)
                        },
                        onRate = {
                            onRate(uiState.appointment.id)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }

        if (confirmCancel) {
            AlertDialog(
                onDismissRequest = {
                    confirmCancel = false
                },
                title = {
                    Text(
                        stringResource(R.string.appointments_cancel_title)
                    )
                },
                text = {
                    Text(
                        stringResource(
                            R.string.appointment_cancel_release_message
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            confirmCancel = false
                            detailViewModel.cancel()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CutTimeRed
                        )
                    ) {
                        Text(
                            stringResource(
                                R.string.appointments_cancel_action
                            )
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            confirmCancel = false
                        }
                    ) {
                        Text(stringResource(R.string.action_keep))
                    }
                }
            )
        }
    }
}
@Composable
private fun AppointmentDetailContent(
    appointment: Appointment,
    isBarberView: Boolean,
    canRate: Boolean,
    isUpdating: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onReschedule: () -> Unit,
    onRate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUpcoming =
        appointment.status == AppointmentStatus.UPCOMING
    val isCompleted =
        appointment.status == AppointmentStatus.COMPLETED

    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                DetailTitle(
                    icon = Icons.Default.ContentCut,
                    title =
                        if (isBarberView) {
                            appointment.customerName
                        } else {
                            appointment.barberName
                        },
                    subtitle = appointment.serviceName
                )
                Spacer(modifier = Modifier.height(18.dp))
                DetailRow(
                    icon = Icons.Default.CalendarMonth,
                    label = stringResource(R.string.booking_summary_date),
                    value =
                        AppointmentDateTime.formatDateForDisplay(
                            appointment.appointmentDate
                        )
                )
                DetailRow(
                    icon = Icons.Default.Schedule,
                    label = stringResource(R.string.booking_summary_time),
                    value = pluralStringResource(
                        R.plurals.appointments_time_duration,
                        appointment.durationMinutes,
                        appointment.appointmentTime,
                        appointment.durationMinutes
                    )
                )
                DetailRow(
                    icon = Icons.Default.Person,
                    label = stringResource(R.string.appointment_detail_status),
                    value =
                        when {
                            appointment.status ==
                                AppointmentStatus.CANCELLED ->
                                stringResource(R.string.appointments_cancelled)

                            isCompleted ->
                                stringResource(R.string.appointments_completed)

                            else ->
                                stringResource(R.string.appointments_upcoming)
                        }
                )
                DetailRow(
                    icon = Icons.Default.ContentCut,
                    label = stringResource(R.string.appointment_detail_price),
                    value = stringResource(
                        R.string.appointments_price,
                        appointment.price
                    )
                )
            }
        }

        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error
            )
        }
        successMessage?.let {
            Text(
                text = it,
                color = CutTimeSuccess
            )
        }

        if (isUpcoming) {
            if (!isBarberView) {
                Button(
                    onClick = onReschedule,
                    enabled = !isUpdating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeNavy
                    )
                ) {
                    Text(stringResource(R.string.action_reschedule))
                }
            } else {
                Button(
                    onClick = onComplete,
                    enabled = !isUpdating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeNavy
                    )
                ) {
                    Text(stringResource(R.string.action_mark_completed))
                }
            }

            OutlinedButton(
                onClick = onCancel,
                enabled = !isUpdating,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text(stringResource(R.string.appointments_cancel_action))
            }
        } else if (
            !isBarberView &&
            canRate &&
            appointment.status == AppointmentStatus.COMPLETED &&
            appointment.ratingId == null
        ) {
            Button(
                onClick = onRate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                Text(stringResource(R.string.appointment_rate_barber))
            }
        } else if (
            !isBarberView &&
            appointment.status == AppointmentStatus.COMPLETED &&
            appointment.ratingId != null
        ) {
            Text(
                text = stringResource(R.string.appointment_review_submitted),
                color = CutTimeSuccess,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DetailTitle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CutTimeNavy,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = title,
                color = CutTimeNavy,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = CutTimeTextSecondary
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CutTimeNavy,
            modifier = Modifier.size(19.dp)
        )
        Spacer(modifier = Modifier.size(9.dp))
        Text(
            text = label,
            color = CutTimeTextSecondary,
            maxLines = 1,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = value,
            color = CutTimeNavy,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End,
            lineHeight = 21.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
