package com.fadynemer.cutime.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadynemer.cutime.notifications.NotificationRegistrationManager
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.NotificationPreferencesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    isBarberMode: Boolean,
    onBack: () -> Unit,
    preferencesViewModel:
        NotificationPreferencesViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState = preferencesViewModel.uiState
    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            preferencesViewModel.setPushEnabled(granted)
            if (granted) {
                NotificationRegistrationManager.sync(context)
            }
        }
    val hasPermission =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Notification Settings",
                        color = CutTimeNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CutTimeNavy
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CutTimeNavy)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                NotificationSettingsIntro(
                    isBarberMode = isBarberMode,
                    permissionGranted = hasPermission
                )

                SettingsToggleCard(
                    title = "Push notifications",
                    description =
                        "Show appointment updates on this device.",
                    checked =
                        uiState.draft.pushEnabled &&
                            hasPermission,
                    onCheckedChange = { enabled ->
                        if (
                            enabled &&
                            Build.VERSION.SDK_INT >=
                            Build.VERSION_CODES.TIRAMISU &&
                            !hasPermission
                        ) {
                            permissionLauncher.launch(
                                Manifest.permission
                                    .POST_NOTIFICATIONS
                            )
                        } else {
                            preferencesViewModel
                                .setPushEnabled(enabled)
                        }
                    }
                )

                SettingsToggleCard(
                    title = "Appointment reminders",
                    description =
                        "Receive reminders 2 hours and 30 minutes before upcoming bookings.",
                    checked = uiState.draft.remindersEnabled,
                    enabled = uiState.draft.pushEnabled,
                    onCheckedChange =
                        preferencesViewModel::setRemindersEnabled
                )

                if (uiState.draft.remindersEnabled) {
                    ReminderScheduleCard()
                }

                SettingsToggleCard(
                    title = "Appointment updates",
                    description =
                        "Receive booking, rescheduling, cancellation, and completion updates.",
                    checked =
                        uiState.draft
                            .appointmentUpdatesEnabled,
                    onCheckedChange =
                        preferencesViewModel::
                            setAppointmentUpdatesEnabled
                )

                SettingsToggleCard(
                    title = "Review prompts",
                    description =
                        "Receive a reminder to review completed appointments.",
                    checked =
                        uiState.draft.reviewPromptsEnabled,
                    onCheckedChange =
                        preferencesViewModel::
                            setReviewPromptsEnabled
                )

                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.successMessage?.let { message ->
                    Row(
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = CutTimeSuccess
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(
                            text = message,
                            color = CutTimeSuccess
                        )
                    }
                }

                Button(
                    onClick = {
                        preferencesViewModel.save()
                        if (
                            uiState.draft.pushEnabled &&
                            hasPermission
                        ) {
                            NotificationRegistrationManager
                                .sync(context)
                        }
                    },
                    enabled =
                        uiState.hasChanges &&
                            !uiState.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CutTimeNavy
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color =
                                MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Save Settings")
                    }
                }

                if (uiState.hasChanges) {
                    OutlinedButton(
                        onClick =
                            preferencesViewModel::discardChanges,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Discard Changes")
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationSettingsIntro(
    isBarberMode: Boolean,
    permissionGranted: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = CutTimeNavy.copy(alpha = 0.07f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = CutTimeNavy,
                modifier = Modifier.size(38.dp)
            )
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(
                    text =
                        if (isBarberMode) {
                            "Barber notifications"
                        } else {
                            "Customer notifications"
                        },
                    color = CutTimeNavy,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text =
                        if (permissionGranted) {
                            "System notification permission is enabled."
                        } else {
                            "Enable system permission to receive alerts."
                        },
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.size(3.dp))
                Text(
                    text = description,
                    color = CutTimeTextSecondary,
                    fontSize = 13.sp
                )
            }
            Switch(
                checked = checked,
                enabled = enabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun ReminderScheduleCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Reminder schedule",
                color = CutTimeNavy,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text =
                    "You will receive two reminders for each upcoming appointment:",
                color = CutTimeTextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.size(10.dp))
            ReminderScheduleRow("2 hours before")
            ReminderScheduleRow("30 minutes before")
        }
    }
}

@Composable
private fun ReminderScheduleRow(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = CutTimeSuccess,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.size(10.dp))
        Text(
            text = label,
            color = CutTimeNavy,
            fontWeight = FontWeight.Medium
        )
    }
}
