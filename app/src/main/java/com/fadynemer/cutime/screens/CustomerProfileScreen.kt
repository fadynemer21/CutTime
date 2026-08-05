package com.fadynemer.cutime.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadynemer.cutime.components.CustomerBottomBar
import com.fadynemer.cutime.R
import com.fadynemer.cutime.components.CustomerDestination
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AccountModePreferences
import com.fadynemer.cutime.viewmodel.CustomerProfileViewModel
import com.fadynemer.cutime.viewmodel.AccountDeletionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(
    onHomeSelected: () -> Unit,
    onAppointmentsSelected: () -> Unit,
    onReturnToBarberMode: () -> Unit,
    profileViewModel: CustomerProfileViewModel = viewModel(),
    deletionViewModel: AccountDeletionViewModel = viewModel(),
) {
    val uiState = profileViewModel.uiState
    val deletionState = deletionViewModel.uiState
    val context = LocalContext.current

    if (deletionState.showConfirmation) {
        AlertDialog(
            onDismissRequest = deletionViewModel::dismissConfirmation,
            title = {
                Text(stringResource(R.string.deletion_confirm_title))
            },
            text = {
                Text(stringResource(R.string.deletion_confirm_message))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deletionViewModel.submit(
                            uiState.profile?.role.orEmpty()
                        )
                    },
                    enabled = !deletionState.isSubmitting
                ) {
                    if (deletionState.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.deletion_submit_action))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = deletionViewModel::dismissConfirmation,
                    enabled = !deletionState.isSubmitting
                ) {
                    Text(stringResource(R.string.action_keep_account))
                }
            }
        )
    }

    if (uiState.isEditing) {
        AlertDialog(
            onDismissRequest = {
                if (!uiState.isSaving) {
                    profileViewModel.cancelEditing()
                }
            },
            title = { Text(stringResource(R.string.profile_edit_name_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = uiState.editedName,
                        onValueChange =
                            profileViewModel::updateEditedName,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isSaving,
                        singleLine = true,
                        label = { Text(stringResource(R.string.field_full_name)) },
                        supportingText = {
                            Text(
                                text =
                                    uiState.editError
                                        ?: "${uiState.editedName.length}/60",
                                color =
                                    if (uiState.editError != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        CutTimeTextSecondary
                                    }
                            )
                        },
                        isError = uiState.editError != null
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = profileViewModel::saveName,
                    enabled = !uiState.isSaving
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = profileViewModel::cancelEditing,
                    enabled = !uiState.isSaving
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.profile_title),
                        color = CutTimeNavy,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )
        },
        bottomBar = {
            CustomerBottomBar(
                selectedDestination = CustomerDestination.PROFILE,
                onHomeSelected = onHomeSelected,
                onAppointmentsSelected = onAppointmentsSelected,
                onProfileSelected = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = CutTimeNavy)
                }

                uiState.profile == null -> {
                    Text(
                        text =
                            uiState.errorMessage
                                ?: "Profile unavailable.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                else -> {
                    val profile = uiState.profile
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = CutTimeNavy,
                        modifier = Modifier.height(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = profile.fullName,
                        color = CutTimeNavy,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = profile.email,
                        color = CutTimeTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = profileViewModel::beginEditing
                    ) {
                        Text(stringResource(R.string.profile_edit_name_action))
                    }
                    uiState.saveMessage?.let { message ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(R.string.profile_account_type),
                                color = CutTimeTextSecondary
                            )
                            Text(
                                text =
                                    if (profile.role == "BARBER") {
                                        "Barber • Customer Mode"
                                    } else {
                                        "Customer"
                                    },
                                color = CutTimeNavy,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = stringResource(
                                    R.string.deletion_section_title
                                ),
                                color = CutTimeNavy,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text =
                                    if (deletionState.request != null) {
                                        stringResource(
                                            R.string.deletion_pending_message
                                        )
                                    } else {
                                        stringResource(
                                            R.string.deletion_section_message
                                        )
                                    },
                                color = CutTimeTextSecondary,
                                fontSize = 14.sp
                            )
                            deletionState.errorMessage?.let { message ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp
                                )
                            }
                            deletionState.successMessage?.let { message ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = message,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 14.sp
                                )
                            }
                            if (
                                !deletionState.isLoading &&
                                deletionState.request == null
                            ) {
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick =
                                        deletionViewModel::showConfirmation,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        stringResource(
                                            R.string.deletion_request_action
                                        ),
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    if (profile.role == "BARBER") {
                        Button(
                            onClick = {
                                AccountModePreferences.setCustomerMode(
                                    context,
                                    profile.uid,
                                    false
                                )
                                onReturnToBarberMode()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CutTimeNavy
                            )
                        ) {
                            Text(stringResource(R.string.profile_return_barber_mode))
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                }
            }
        }
    }
}
