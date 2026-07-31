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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fadynemer.cutime.components.BarberDestination
import com.fadynemer.cutime.components.BarberManagementScaffold
import com.fadynemer.cutime.model.DayAvailability
import com.fadynemer.cutime.model.WorkingPeriod
import com.fadynemer.cutime.model.effectiveWorkingPeriods
import com.fadynemer.cutime.R
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
                        text = stringResource(R.string.availability_weekly_hours),
                        color = CutTimeNavy,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text =
                            stringResource(R.string.availability_time_format_hint),
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
                            availabilityViewModel::updateDay,
                        onPeriodChange = { periodIndex, period ->
                            availabilityViewModel.updateWorkingPeriod(
                                dayName =
                                    uiState.availability.days[index].day,
                                index = periodIndex,
                                period = period
                            )
                        },
                        onAddPeriod = {
                            availabilityViewModel.addWorkingPeriod(
                                uiState.availability.days[index].day
                            )
                        },
                        onRemovePeriod = { periodIndex ->
                            availabilityViewModel.removeWorkingPeriod(
                                dayName =
                                    uiState.availability.days[index].day,
                                index = periodIndex
                            )
                        }
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
                                text = stringResource(R.string.availability_save),
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
    onChange: (DayAvailability) -> Unit,
    onPeriodChange: (Int, WorkingPeriod) -> Unit,
    onAddPeriod: () -> Unit,
    onRemovePeriod: (Int) -> Unit
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
                Text(
                    text = stringResource(R.string.availability_break_hint),
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp
                )
                day.effectiveWorkingPeriods()
                    .forEachIndexed { index, period ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.availability_work_period,
                                    index + 1
                                ),
                                color = CutTimeNavy,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            if (day.effectiveWorkingPeriods().size > 1) {
                                IconButton(
                                    onClick = { onRemovePeriod(index) },
                                    modifier = Modifier.testTag(
                                        "remove_period_${day.day}_$index"
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = stringResource(
                                            R.string.availability_remove_period,
                                            day.day
                                        ),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement =
                                Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = period.startTime,
                                onValueChange = { value ->
                                    onPeriodChange(
                                        index,
                                        period.copy(
                                            startTime = filterTimeInput(value)
                                        )
                                    )
                                },
                                label = {
                                    Text(stringResource(R.string.availability_starts))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("period_start_${day.day}_$index")
                            )
                            OutlinedTextField(
                                value = period.endTime,
                                onValueChange = { value ->
                                    onPeriodChange(
                                        index,
                                        period.copy(
                                            endTime = filterTimeInput(value)
                                        )
                                    )
                                },
                                label = {
                                    Text(stringResource(R.string.availability_ends))
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("period_end_${day.day}_$index")
                            )
                        }
                    }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onAddPeriod,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("add_period_${day.day}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.availability_add_period))
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
            text = stringResource(R.string.availability_blocked_dates),
            color = CutTimeNavy,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text =
                stringResource(R.string.availability_blocked_dates_hint),
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
                label = { Text(stringResource(R.string.availability_blocked_date)) },
                placeholder = { Text(stringResource(R.string.availability_blocked_date_hint)) },
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
                Text(stringResource(R.string.action_add))
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
