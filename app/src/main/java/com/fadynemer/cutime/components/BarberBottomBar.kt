package com.fadynemer.cutime.components

import androidx.annotation.StringRes
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.vector.ImageVector
import com.fadynemer.cutime.R
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.navigation.AppRoute

enum class BarberDestination(
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val route: String
) {
    DASHBOARD(
        R.string.nav_dashboard,
        Icons.Default.Dashboard,
        AppRoute.BarberDashboard.pattern
    ),
    SERVICES(
        R.string.nav_services,
        Icons.Default.ContentCut,
        AppRoute.BarberServices.pattern
    ),
    AVAILABILITY(
        R.string.nav_hours,
        Icons.Default.CalendarMonth,
        AppRoute.BarberAvailability.pattern
    ),
    PROFILE(
        R.string.nav_profile,
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
            val label = stringResource(destination.labelRes)
            NavigationBarItem(
                selected = destination == selectedDestination,
                onClick = {
                    onDestinationSelected(destination)
                },
                icon = {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = label
                    )
                },
                label = {
                    Text(
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
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
