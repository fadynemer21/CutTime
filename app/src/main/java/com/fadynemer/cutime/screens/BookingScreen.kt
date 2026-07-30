package com.fadynemer.cutime.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Event
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.model.BarberShop
import com.fadynemer.cutime.ui.theme.CutTimeLightGrey
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AppointmentDateTime
import com.fadynemer.cutime.util.AvailabilitySlotGenerator
import com.fadynemer.cutime.viewmodel.BookingAvailabilityViewModel
import com.fadynemer.cutime.viewmodel.BookingUiState
import com.fadynemer.cutime.viewmodel.BookingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class BookingDateOption(
    val value: String,
    val dayName: String,
    val dayNumber: String,
    val monthName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingScreen(
    barberShop: BarberShop?,
    onBack: () -> Unit,
    onViewAppointments: () -> Unit,
    bookingViewModel: BookingViewModel = viewModel(),
    availabilityViewModel:
        BookingAvailabilityViewModel = viewModel()
) {
    val uiState = bookingViewModel.uiState
    val availabilityState = availabilityViewModel.uiState

    LaunchedEffect(
        barberShop?.id,
        uiState.selectedDate
    ) {
        val barberId = barberShop?.id
        val date = uiState.selectedDate

        if (barberId != null && date != null) {
            availabilityViewModel.observe(barberId, date)
        }
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
                            text =
                                when {
                                    uiState.createdAppointmentId != null ->
                                        "Booking Confirmed"

                                    uiState.isReviewing ->
                                        "Review Booking"

                                    else ->
                                        "Book Appointment"
                                },
                            color = CutTimeNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (
                                    uiState.isReviewing &&
                                    uiState.createdAppointmentId == null
                                ) {
                                    bookingViewModel.editBooking()
                                } else {
                                    onBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector =
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = CutTimeNavy
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            when {
                uiState.createdAppointmentId != null &&
                    barberShop != null -> {
                    BookingSuccess(
                        barberShop = barberShop,
                        uiState = uiState,
                        onViewAppointments = onViewAppointments,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                barberShop == null -> {
                    BookingBarberNotFound(
                        onBack = onBack,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                barberShop.isDevelopmentFallback -> {
                    DevelopmentBookingUnavailable(
                        onBack = onBack,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                uiState.isReviewing -> {
                    BookingReview(
                        barberShop = barberShop,
                        uiState = uiState,
                        onEdit = bookingViewModel::editBooking,
                        onSubmit = {
                            bookingViewModel.submitBooking(
                                barberShop
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }

                else -> {
                    BookingForm(
                        barberShop = barberShop,
                        uiState = uiState,
                        occupiedTimes =
                            availabilityState.occupiedTimes,
                        isLoadingTimes =
                            availabilityState.isLoading,
                        availabilityError =
                            availabilityState.errorMessage,
                        onServiceSelected = bookingViewModel::selectService,
                        onDateSelected = bookingViewModel::selectDate,
                        onTimeSelected = bookingViewModel::selectTime,
                        onReview = bookingViewModel::reviewBooking,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
private fun DevelopmentBookingUnavailable(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ContentCut,
            contentDescription = null,
            tint = CutTimeNavy,
            modifier = Modifier.size(52.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Preview barber",
            color = CutTimeNavy,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                "This profile is bundled development data. Booking is disabled so it cannot create an appointment for a barber that does not exist in Firestore.",
            color = CutTimeTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = CutTimeNavy
            )
        ) {
            Text("Back")
        }
    }
}

@Composable
private fun BookingForm(
    barberShop: BarberShop,
    uiState: BookingUiState,
    occupiedTimes: Set<String>,
    isLoadingTimes: Boolean,
    availabilityError: String?,
    onServiceSelected: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onTimeSelected: (String) -> Unit,
    onReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateOptions = remember {
        createDateOptions()
    }
    val selectedService = barberShop.services.find { service ->
        service.id == uiState.selectedServiceId
    }
    val availableTimes =
        uiState.selectedDate
            ?.let { selectedDate ->
                selectedService?.let { service ->
                    AvailabilitySlotGenerator.availableTimes(
                        availability = barberShop.availability,
                        date = LocalDate.parse(selectedDate),
                        durationMinutes =
                            service.durationMinutes,
                        occupiedTimes = occupiedTimes
                    )
                }
            }
            .orEmpty()

    LazyColumn(
        modifier = modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 12.dp,
            bottom = 28.dp
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            Text(
                text = barberShop.name,
                color = CutTimeNavy,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = "Choose a service, date, and available time.",
                color = CutTimeTextSecondary
            )
        }

        item {
            Column {
                BookingSectionTitle(
                    number = "1",
                    title = "Select a service"
                )
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    barberShop.services.forEach { service ->
                        ServiceOption(
                            service = service,
                            selected =
                                service.id == uiState.selectedServiceId,
                            onClick = {
                                onServiceSelected(service.id)
                            }
                        )
                    }
                }
            }
        }

        item {
            Column {
                BookingSectionTitle(
                    number = "2",
                    title = "Select a date"
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = dateOptions,
                        key = { option -> option.value }
                    ) { option ->
                        DateOption(
                            option = option,
                            selected =
                                option.value == uiState.selectedDate,
                            onClick = {
                                onDateSelected(option.value)
                            }
                        )
                    }
                }
            }
        }

        item {
            Column {
                BookingSectionTitle(
                    number = "3",
                    title = "Select a time"
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text =
                        if (uiState.selectedDate == null) {
                            "Choose a date to view available times."
                        } else {
                            "Available times for the selected date"
                        },
                    color = CutTimeTextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.selectedDate == null) {
                    LazyRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        items(barberShop.availableTimes) { time ->
                            TimeOption(
                                time = time,
                                enabled = false,
                                selected = false,
                                onClick = {}
                            )
                        }
                    }
                } else if (isLoadingTimes) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = CutTimeNavy,
                        strokeWidth = 2.dp
                    )
                } else if (availabilityError != null) {
                    Text(
                        text = availabilityError,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                } else if (selectedService == null) {
                    Text(
                        text =
                            "Choose a service to calculate available times.",
                        color = CutTimeTextSecondary,
                        fontSize = 14.sp
                    )
                } else if (availableTimes.isEmpty()) {
                    Text(
                        text =
                            "No remaining times are available on this date.",
                        color = CutTimeTextSecondary,
                        fontSize = 14.sp
                    )
                } else {
                    LazyRow(
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        items(availableTimes) { time ->
                            TimeOption(
                                time = time,
                                enabled = true,
                                selected =
                                    time == uiState.selectedTime,
                                onClick = {
                                    onTimeSelected(time)
                                }
                            )
                        }
                    }
                }
            }
        }

        if (selectedService != null) {
            item {
                BookingSummaryCard(
                    service = selectedService,
                    selectedDate = uiState.selectedDate,
                    selectedTime = uiState.selectedTime
                )
            }
        }

        item {
            Button(
                onClick = onReview,
                enabled = uiState.canReview,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                Text(
                    text = "Review Booking",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun ServiceOption(
    service: BarberService,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) {
                    CutTimeLightGrey
                } else {
                    MaterialTheme.colorScheme.surface
                }
        ),
        border =
            if (selected) {
                BorderStroke(2.dp, CutTimeNavy)
            } else {
                null
            },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ContentCut,
                contentDescription = null,
                tint = CutTimeNavy,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${service.durationMinutes} minutes",
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp
                )
            }

            Text(
                text = "₪${service.price}",
                color = CutTimeNavy,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DateOption(
    option: BookingDateOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
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
                BorderStroke(1.dp, CutTimeLightGrey)
            },
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = option.dayName,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        CutTimeTextSecondary
                    },
                fontSize = 12.sp
            )
            Text(
                text = option.dayNumber,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        CutTimeNavy
                    },
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = option.monthName,
                color =
                    if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        CutTimeTextSecondary
                    },
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TimeOption(
    time: String,
    enabled: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.clickable(
            enabled = enabled,
            onClick = onClick
        ),
        shape = RoundedCornerShape(12.dp),
        color =
            if (selected) {
                CutTimeNavy
            } else {
                CutTimeLightGrey
            }
    ) {
        Text(
            text = time,
            color =
                when {
                    !enabled -> CutTimeTextSecondary.copy(alpha = 0.5f)
                    selected -> MaterialTheme.colorScheme.onPrimary
                    else -> CutTimeNavy
                },
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(
                horizontal = 19.dp,
                vertical = 12.dp
            )
        )
    }
}

@Composable
private fun BookingSummaryCard(
    service: BarberService,
    selectedDate: String?,
    selectedTime: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Booking summary",
                color = CutTimeNavy,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow("Service", service.name)
            SummaryRow("Duration", "${service.durationMinutes} minutes")
            SummaryRow(
                "Date",
                selectedDate
                    ?.let(
                        AppointmentDateTime::formatDateForDisplay
                    )
                    ?: "Not selected"
            )
            SummaryRow("Time", selectedTime ?: "Not selected")
            SummaryRow("Total", "₪${service.price}", emphasized = true)
        }
    }
}

@Composable
private fun BookingReview(
    barberShop: BarberShop,
    uiState: BookingUiState,
    onEdit: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedService = barberShop.services.first { service ->
        service.id == uiState.selectedServiceId
    }

    LazyColumn(
        modifier = modifier.navigationBarsPadding(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 24.dp,
            bottom = 28.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = CutTimeRed,
                modifier = Modifier.size(58.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Check your appointment",
                color = CutTimeNavy,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = "Review the details before the appointment is submitted.",
                color = CutTimeTextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 3.dp
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    SummaryRow("Barber", barberShop.name)
                    SummaryRow("Service", selectedService.name)
                    SummaryRow(
                        "Duration",
                        "${selectedService.durationMinutes} minutes"
                    )
                    SummaryRow(
                        "Date",
                        AppointmentDateTime.formatDateForDisplay(
                            uiState.selectedDate.orEmpty()
                        )
                    )
                    SummaryRow("Time", uiState.selectedTime.orEmpty())
                    SummaryRow(
                        label = "Total",
                        value = "₪${selectedService.price}",
                        emphasized = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (uiState.errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = uiState.errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            Button(
                onClick = onSubmit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                if (uiState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = "Confirm Appointment",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onEdit,
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Edit Selection")
            }
        }
    }
}

@Composable
private fun BookingSuccess(
    barberShop: BarberShop,
    uiState: BookingUiState,
    onViewAppointments: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedService =
        barberShop.services.first { service ->
            service.id == uiState.selectedServiceId
        }

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = CutTimeRed,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "Appointment booked",
            color = CutTimeNavy,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text =
                "Your appointment with ${barberShop.name} is confirmed.",
            color = CutTimeTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                SummaryRow("Service", selectedService.name)
                SummaryRow(
                    "Date",
                    AppointmentDateTime.formatDateForDisplay(
                        uiState.selectedDate.orEmpty()
                    )
                )
                SummaryRow("Time", uiState.selectedTime.orEmpty())
                SummaryRow(
                    "Total",
                    "₪${selectedService.price}",
                    emphasized = true
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onViewAppointments,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = CutTimeNavy
            )
        ) {
            Text(
                text = "View My Appointments",
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    emphasized: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = CutTimeTextSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = CutTimeNavy,
            fontWeight =
                if (emphasized) {
                    FontWeight.Bold
                } else {
                    FontWeight.Medium
                }
        )
    }
}

@Composable
private fun BookingSectionTitle(
    number: String,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(28.dp),
            shape = RoundedCornerShape(9.dp),
            color = CutTimeNavy
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = number,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(9.dp))
        Text(
            text = title,
            color = CutTimeNavy,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun BookingBarberNotFound(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Event,
            contentDescription = null,
            tint = CutTimeNavy,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Booking unavailable",
            color = CutTimeNavy,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This barber could not be found.",
            color = CutTimeTextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(
                containerColor = CutTimeNavy
            )
        ) {
            Text("Go Back")
        }
    }
}

private fun createDateOptions(): List<BookingDateOption> {
    val dayFormatter =
        DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH)
    val monthFormatter =
        DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH)

    return (0L..6L).map { dayOffset ->
        val date = LocalDate.now().plusDays(dayOffset)

        BookingDateOption(
            value = date.format(DateTimeFormatter.ISO_LOCAL_DATE),
            dayName =
                if (dayOffset == 0L) {
                    "Today"
                } else {
                    date.format(dayFormatter)
                },
            dayNumber = date.dayOfMonth.toString(),
            monthName = date.format(monthFormatter)
        )
    }
}
