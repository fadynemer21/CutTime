package com.fadynemer.cutime.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.viewmodel.SessionViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    sessionViewModel: SessionViewModel = viewModel()
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Customer Home",
            color = CutTimeNavy,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Available barbers will appear here."
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                sessionViewModel.logout()

                navController.navigate("welcome") {
                    popUpTo("home") {
                        inclusive = true
                    }

                    launchSingleTop = true
                }
            }
        ) {
            Text("Logout")
        }
    }
}