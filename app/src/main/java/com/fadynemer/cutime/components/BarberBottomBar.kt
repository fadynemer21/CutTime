package com.fadynemer.cutime.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.navigation.AppRoute

enum class BarberDestination(
    val label: String,
    val icon: ImageVector,
    val route: String
) {
    DASHBOARD(
        "Dashboard",
        Icons.Default.Dashboard,
        AppRoute.BarberDashboard.pattern
    ),
    SERVICES(
        "Services",
        Icons.Default.ContentCut,
        AppRoute.BarberServices.pattern
    ),
    AVAILABILITY(
        "Availability",
        Icons.Default.CalendarMonth,
        AppRoute.BarberAvailability.pattern
    ),
    PROFILE(
        "Profile",
        Icons.Default.Person,
        AppRoute.BarberManageProfile.pattern
    )
}

@Composable
fun BarberBottomBar(
    selectedDestination: BarberDestination,
    onDestinationSelected: (BarberDestination) -> Unit
) {
    NavigationBar {
        BarberDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = {
                    onDestinationSelected(destination)
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = destination.label
                    )
                },
                label = {
                    Text(destination.label)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = CutTimeRed,
                    selectedTextColor = CutTimeNavy,
                    indicatorColor =
                        CutTimeNavy.copy(alpha = 0.1f),
                    unselectedIconColor =
                        CutTimeNavy.copy(alpha = 0.65f),
                    unselectedTextColor =
                        CutTimeNavy.copy(alpha = 0.65f)
                )
            )
        }
    }
}
