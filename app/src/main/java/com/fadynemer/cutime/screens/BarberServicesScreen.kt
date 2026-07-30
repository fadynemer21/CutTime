package com.fadynemer.cutime.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fadynemer.cutime.model.BarberService
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.viewmodel.BarberServicesViewModel

@Composable
fun BarberServicesScreen(
    navController: NavController,
    servicesViewModel: BarberServicesViewModel = viewModel()
) {
    var editingService by remember {
        mutableStateOf<BarberService?>(null)
    }
    var showEditor by remember {
        mutableStateOf(false)
    }
    var deletingService by remember {
        mutableStateOf<BarberService?>(null)
    }

    BarberManagementScaffold(
        title = "Services",
        selectedDestination = BarberDestination.SERVICES,
        onDestinationSelected = { destination ->
            navigateToBarberDestination(navController, destination)
        }
    ) { innerPadding ->
        val uiState = servicesViewModel.uiState

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = CutTimeNavy
                    )
                }

                uiState.services.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No services yet",
                            color = CutTimeNavy,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text =
                                "Add the services customers can book.",
                            color = CutTimeTextSecondary
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding =
                            androidx.compose.foundation.layout.PaddingValues(
                                20.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text =
                                    "Manage prices and appointment durations.",
                                color = CutTimeTextSecondary,
                                modifier = Modifier.padding(
                                    bottom = 6.dp
                                )
                            )
                        }
                        items(
                            count = uiState.services.size,
                            key = { index ->
                                uiState.services[index].id
                            }
                        ) { index ->
                            val service = uiState.services[index]
                            ServiceManagementCard(
                                service = service,
                                onEdit = {
                                    editingService = service
                                    showEditor = true
                                    servicesViewModel.clearMessage()
                                },
                                onDelete = {
                                    deletingService = service
                                }
                            )
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
                        uiState.successMessage?.let { message ->
                            item {
                                Text(
                                    text = message,
                                    color = CutTimeSuccess
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = {
                    editingService = null
                    showEditor = true
                    servicesViewModel.clearMessage()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
                containerColor = CutTimeNavy,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add service"
                )
            }
        }
    }

    if (showEditor) {
        ServiceEditorDialog(
            service = editingService,
            isSaving = servicesViewModel.uiState.isSaving,
            errorMessage =
                servicesViewModel.uiState.errorMessage,
            onDismiss = {
                if (!servicesViewModel.uiState.isSaving) {
                    showEditor = false
                }
            },
            onSave = { service ->
                servicesViewModel.saveService(service) {
                    showEditor = false
                }
            }
        )
    }

    deletingService?.let { service ->
        AlertDialog(
            onDismissRequest = {
                deletingService = null
            },
            title = {
                Text("Remove service?")
            },
            text = {
                Text(
                    "Customers will no longer be able to book ${service.name}."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        servicesViewModel.deleteService(service.id)
                        deletingService = null
                    }
                ) {
                    Text("Remove", color = CutTimeRed)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingService = null
                    }
                ) {
                    Text("Keep")
                }
            }
        )
    }
}

@Composable
private fun ServiceManagementCard(
    service: BarberService,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = service.name,
                    color = CutTimeNavy,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 17.sp
                )
                Text(
                    text =
                        "${service.durationMinutes} minutes • ₪${service.price}",
                    color = CutTimeTextSecondary,
                    fontSize = 14.sp
                )
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit ${service.name}",
                    tint = CutTimeNavy
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove ${service.name}",
                    tint = CutTimeRed
                )
            }
        }
    }
}

@Composable
private fun ServiceEditorDialog(
    service: BarberService?,
    isSaving: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSave: (BarberService) -> Unit
) {
    var name by remember(service?.id) {
        mutableStateOf(service?.name.orEmpty())
    }
    var price by remember(service?.id) {
        mutableStateOf(service?.price?.toString().orEmpty())
    }
    var duration by remember(service?.id) {
        mutableStateOf(
            service?.durationMinutes?.toString() ?: "30"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (service == null) {
                    "Add Service"
                } else {
                    "Edit Service"
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Service name") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = price,
                    onValueChange = {
                        price = it.filter(Char::isDigit)
                    },
                    label = { Text("Price (₪)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = {
                        duration = it.filter(Char::isDigit)
                    },
                    label = { Text("Duration in minutes") },
                    supportingText = {
                        Text("Use a 15-minute interval.")
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        BarberService(
                            id = service?.id.orEmpty(),
                            name = name,
                            price = price.toIntOrNull() ?: 0,
                            durationMinutes =
                                duration.toIntOrNull() ?: 0
                        )
                    )
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CutTimeNavy
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isSaving
            ) {
                Text("Cancel")
            }
        }
    )
}
