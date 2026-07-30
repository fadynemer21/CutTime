package com.fadynemer.cutime.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fadynemer.cutime.components.BarberDestination
import com.fadynemer.cutime.components.BarberManagementScaffold
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeSuccess
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.AccountModePreferences
import com.fadynemer.cutime.viewmodel.BarberProfileViewModel
import com.google.firebase.auth.FirebaseAuth

@Composable
fun BarberManageProfileScreen(
    navController: NavController,
    profileViewModel: BarberProfileViewModel = viewModel()
) {
    val context = LocalContext.current
    BarberManagementScaffold(
        title = "Manage Profile",
        selectedDestination = BarberDestination.PROFILE,
        onDestinationSelected = { destination ->
            navigateToBarberDestination(navController, destination)
        }
    ) { innerPadding ->
        val uiState = profileViewModel.uiState

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    text = "Public barber profile",
                    color = CutTimeNavy,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text =
                        "This information will be shown to customers.",
                    color = CutTimeTextSecondary
                )
                Spacer(modifier = Modifier.height(22.dp))

                OutlinedTextField(
                    value = uiState.shopName,
                    onValueChange = profileViewModel::updateShopName,
                    label = { Text("Shop name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = profileViewModel::updateDescription,
                    label = { Text("Description") },
                    minLines = 5,
                    supportingText = {
                        Text("${uiState.description.length}/500")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                uiState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                uiState.successMessage?.let { message ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        color = CutTimeSuccess
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                Button(
                    onClick = profileViewModel::save,
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
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Save Profile",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = {
                        val uid =
                            FirebaseAuth.getInstance()
                                .currentUser?.uid
                        if (uid != null) {
                            AccountModePreferences.setCustomerMode(
                                context,
                                uid,
                                true
                            )
                            navController.navigate("home") {
                                popUpTo("dashboard") {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Switch to Customer Mode")
                }
            }
        }
    }
}
