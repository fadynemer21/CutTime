package com.fadynemer.cutime.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.fadynemer.cutime.R
import com.fadynemer.cutime.viewmodel.SplashViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    splashViewModel: SplashViewModel = viewModel()
) {
    val uiState = splashViewModel.uiState

    var minimumDelayComplete by remember {
        mutableStateOf(false)
    }

    /*
     * Keep the logo visible for at least 1.5 seconds.
     */
    LaunchedEffect(Unit) {
        delay(1500)
        minimumDelayComplete = true
    }

    /*
     * Navigate only after both the minimum logo time and
     * the Firebase session check have finished.
     */
    LaunchedEffect(
        minimumDelayComplete,
        uiState.destination
    ) {
        val destination = uiState.destination

        if (
            minimumDelayComplete &&
            destination != null
        ) {
            navController.navigate(destination) {
                popUpTo("splash") {
                    inclusive = true
                }

                launchSingleTop = true
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                MaterialTheme.colorScheme.background
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(
                id = R.drawable.cutime_logo
            ),
            contentDescription = "CutTime Logo",
            modifier = Modifier.size(600.dp)
        )
    }
}