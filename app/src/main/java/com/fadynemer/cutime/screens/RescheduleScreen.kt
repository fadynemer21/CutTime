package com.fadynemer.cutime.screens

import com.fadynemer.cutime.R

import androidx.compose.ui.res.stringResource

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadynemer.cutime.data.BarberCatalogCache
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AvailabilitySlotGenerator
import com.fadynemer.cutime.util.AppointmentDateTime
import com.fadynemer.cutime.viewmodel.BookingAvailabilityViewModel
import com.fadynemer.cutime.viewmodel.RescheduleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RescheduleScreen(
    appointmentId: String,
    onBack: () -> Unit,
    onFinished: () -> Unit,
    rescheduleViewModel: RescheduleViewModel = viewModel(),
    availabilityViewModel:
        BookingAvailabilityViewModel = viewModel()
) {
    val uiState = rescheduleViewModel.uiState
    val availabilityState = availabilityViewModel.uiState

    LaunchedEffect(appointmentId) {
        rescheduleViewModel.observe(appointmentId)
    }
    LaunchedEffect(
        uiState.appointment?.barberId,
        uiState.selectedDate
    ) {
        val barberId = uiState.appointment?.barberId
        val date = uiState.selectedDate

        if (barberId != null && date != null) {
            availabilityViewModel.observe(barberId, date)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reschedule_title),
                        color = CutTimeNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector =
                                Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
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

            uiState.isSuccessful -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = CutTimeRed,
                        modifier = Modifier.size(70.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.reschedule_success),
                        color = CutTimeNavy,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onFinished,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CutTimeNavy
                        )
                    ) {
                        Text(stringResource(R.string.action_view_appointment))
                    }
                }
            }

            uiState.appointment == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text =
                            uiState.errorMessage
                                ?: "Appointment unavailable.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            else -> {
                val appointment = uiState.appointment
                val barber =
                    BarberCatalogCache.find(appointment.barberId)
                val dates = remember { nextDates() }
                val times =
                    if (
                        barber != null &&
                        uiState.selectedDate != null
                    ) {
                        val occupiedTimes =
                            if (
                                uiState.selectedDate ==
                                appointment.appointmentDate
                            ) {
                                availabilityState.occupiedTimes -
                                    AppointmentDateTime.reservedTimes(
                                        time =
                                            appointment.appointmentTime,
                                        durationMinutes =
                                            appointment.durationMinutes
                                    ).toSet()
                            } else {
                                availabilityState.occupiedTimes
                            }
                        AvailabilitySlotGenerator.availableTimes(
                            availability = barber.availability,
                            date =
                                LocalDate.parse(
                                    uiState.selectedDate
                                ),
                            durationMinutes =
                                appointment.durationMinutes,
                            occupiedTimes = occupiedTimes
                        )
                    } else {
                        emptyList()
                    }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(22.dp)
                ) {
                    item {
                        Text(
                            text = appointment.serviceName,
                            color = CutTimeNavy,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text =
                                "${appointment.durationMinutes} minutes with ${appointment.barberName}",
                            color = CutTimeTextSecondary
                        )
                    }

                    item {
                        Text(
                            text = stringResource(R.string.reschedule_choose_date),
                            color = CutTimeNavy,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyRow(
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            items(dates) { date ->
                                SelectableValue(
                                    value =
                                        date.format(
                                            DateTimeFormatter.ofPattern(
                                                "EEE d MMM",
                                                Locale.ENGLISH
                                            )
                                        ),
                                    selected =
                                        uiState.selectedDate ==
                                            date.toString(),
                                    onClick = {
                                        rescheduleViewModel.selectDate(
                                            date.toString()
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = stringResource(R.string.reschedule_choose_time),
                            color = CutTimeNavy,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        when {
                            uiState.selectedDate == null ->
                                Text(
                                    stringResource(R.string.reschedule_select_date_first),
                                    color = CutTimeTextSecondary
                                )

                            availabilityState.isLoading ->
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = CutTimeNavy,
                                    strokeWidth = 2.dp
                                )

                            times.isEmpty() ->
                                Text(
                                    stringResource(R.string.reschedule_no_times),
                                    color = CutTimeTextSecondary
                                )

                            else ->
                                LazyRow(
                                    horizontalArrangement =
                                        Arrangement.spacedBy(10.dp)
                                ) {
                                    items(times) { time ->
                                        SelectableValue(
                                            value = time,
                                            selected =
                                                uiState.selectedTime ==
                                                    time,
                                            onClick = {
                                                rescheduleViewModel
                                                    .selectTime(time)
                                            }
                                        )
                                    }
                                }
                        }
                    }

                    uiState.errorMessage?.let { message ->
                        item {
                            Text(
                                text = message,
                                color =
                                    MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    item {
                        Button(
                            onClick = rescheduleViewModel::submit,
                            enabled = uiState.canSubmit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CutTimeNavy
                            )
                        ) {
                            if (uiState.isSubmitting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color =
                                        MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(stringResource(R.string.reschedule_confirm_time))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectableValue(
    value: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color =
            if (selected) {
                CutTimeNavy
            } else {
                MaterialTheme.colorScheme.surface
            },
        border =
            if (selected) {
                null
            } else {
                BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant
                )
            }
    ) {
        Text(
            text = value,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    CutTimeNavy
                },
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 13.dp
            )
        )
    }
}

private fun nextDates(): List<LocalDate> {
    val today = LocalDate.now()
    return (0L until 21L).map(today::plusDays)
}
