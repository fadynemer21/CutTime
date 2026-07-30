package com.fadynemer.cutime.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import com.fadynemer.cutime.ui.theme.CutTimeNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarberManagementScaffold(
    title: String,
    selectedDestination: BarberDestination,
    onDestinationSelected: (BarberDestination) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
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
                            text = title,
                            color = CutTimeNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                )
            },
            bottomBar = {
                BarberBottomBar(
                    selectedDestination = selectedDestination,
                    onDestinationSelected =
                        onDestinationSelected
                )
            },
            content = content
        )
    }
}
