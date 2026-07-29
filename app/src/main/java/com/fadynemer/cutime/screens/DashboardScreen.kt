package com.fadynemer.cutime.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fadynemer.cutime.ui.theme.CutTimeNavy

@Composable
fun DashboardScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Barber Dashboard",
            color = CutTimeNavy,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Appointments and availability will appear here."
        )
    }
}