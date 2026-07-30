package com.fadynemer.cutime.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fadynemer.cutime.components.BarberDestination
import com.fadynemer.cutime.components.BarberManagementScaffold
import com.fadynemer.cutime.model.DayAvailability
import com.fadynemer.cutime.ui.theme.CutTimeLightGrey
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.BarberAvailabilityViewModel

@Composable
fun BarberAvailabilityScreen(
    navController: NavController,
    availabilityViewModel:
        BarberAvailabilityViewModel = viewModel()
) {
    BarberManagementScaffold(
        title = "Availability",
        selectedDestination = BarberDestination.AVAILABILITY,
        onDestinationSelected = { destination ->
            navigateToBarberDestination(navController, destination)
        }
    ) { innerPadding ->
        val uiState = availabilityViewModel.uiState

        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(color = CutTimeNavy)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        20.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "Weekly working hours",
                        color = CutTimeNavy,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text =
                            "Times use the 24-hour HH:mm format.",
                        color = CutTimeTextSecondary
                    )
                }

                items(
                    count = uiState.availability.days.size,
                    key = { index ->
                        uiState.availability.days[index].day
                    }
                ) { index ->
                    DayAvailabilityCard(
                        day = uiState.availability.days[index],
                        onChange =
                            availabilityViewModel::updateDay
                    )
                }

                item {
                    BlockedDatesSection(
                        blockedDates =
                            uiState.availability.blockedDates,
                        onAdd =
                            availabilityViewModel::addBlockedDate,
                        onRemove =
                            availabilityViewModel::removeBlockedDate
                    )
                }

                uiState.errorMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                uiState.successMessage?.let { message ->
                    item {
                        Text(
                            text = message,
                            color = CutTimeSuccess
                        )
                    }
                }

                item {
                    Button(
                        onClick = availabilityViewModel::save,
                        enabled = !uiState.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CutTimeNavy
                        )
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(22.dp),
                                color =
                                    MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save Availability",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayAvailabilityCard(
    day: DayAvailability,
    onChange: (DayAvailability) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = day.day,
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text =
                        if (day.isOpen) "Open" else "Closed",
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Switch(
                    checked = day.isOpen,
                    onCheckedChange = { isOpen ->
                        onChange(day.copy(isOpen = isOpen))
                    }
                )
            }

            if (day.isOpen) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = day.startTime,
                        onValueChange = { value ->
                            onChange(
                                day.copy(
                                    startTime =
                                        filterTimeInput(value)
                                )
                            )
                        },
                        label = { Text("Opens") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = day.endTime,
                        onValueChange = { value ->
                            onChange(
                                day.copy(
                                    endTime =
                                        filterTimeInput(value)
                                )
                            )
                        },
                        label = { Text("Closes") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockedDatesSection(
    blockedDates: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var value by remember {
        mutableStateOf("")
    }

    Column {
        Text(
            text = "Blocked dates",
            color = CutTimeNavy,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text =
                "Add holidays or days off using YYYY-MM-DD.",
            color = CutTimeTextSecondary,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = {
                    value =
                        it.filter { character ->
                            character.isDigit() ||
                                character == '-'
                        }.take(10)
                },
                label = { Text("Blocked date") },
                placeholder = { Text("2026-08-15") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = {
                    onAdd(value)
                    value = ""
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                Text("Add")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        blockedDates.forEach { blockedDate ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = CutTimeLightGrey
            ) {
                Row(
                    modifier = Modifier.padding(
                        start = 14.dp,
                        end = 4.dp,
                        top = 5.dp,
                        bottom = 5.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = blockedDate,
                        color = CutTimeNavy,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            onRemove(blockedDate)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription =
                                "Remove blocked date $blockedDate",
                            tint = CutTimeNavy
                        )
                    }
                }
            }
        }
    }
}

private fun filterTimeInput(
    value: String
): String {
    val digits = value.filter(Char::isDigit).take(4)

    return if (digits.length <= 2) {
        digits
    } else {
        "${digits.take(2)}:${digits.drop(2)}"
    }
}
