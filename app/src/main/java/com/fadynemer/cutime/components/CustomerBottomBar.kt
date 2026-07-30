package com.fadynemer.cutime.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed

enum class CustomerDestination {
    HOME,
    APPOINTMENTS,
    PROFILE
}

@Composable
fun CustomerBottomBar(
    selectedDestination: CustomerDestination,
    onHomeSelected: () -> Unit,
    onAppointmentsSelected: () -> Unit,
    onProfileSelected: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected =
                selectedDestination == CustomerDestination.HOME,
            onClick = onHomeSelected,
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Home"
                )
            },
            label = {
                Text("Home")
            },
            colors = customerNavigationColors()
        )

        NavigationBarItem(
            selected =
                selectedDestination ==
                    CustomerDestination.APPOINTMENTS,
            onClick = onAppointmentsSelected,
            icon = {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Appointments"
                )
            },
            label = {
                Text("Appointments")
            },
            colors = customerNavigationColors()
        )

        NavigationBarItem(
            selected =
                selectedDestination == CustomerDestination.PROFILE,
            onClick = onProfileSelected,
            icon = {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile"
                )
            },
            label = {
                Text("Profile")
            },
            colors = customerNavigationColors()
        )
    }
}

@Composable
private fun customerNavigationColors() =
    NavigationBarItemDefaults.colors(
        selectedIconColor = CutTimeRed,
        selectedTextColor = CutTimeNavy,
        indicatorColor = CutTimeNavy.copy(alpha = 0.1f),
        unselectedIconColor = CutTimeNavy.copy(alpha = 0.65f),
        unselectedTextColor = CutTimeNavy.copy(alpha = 0.65f)
    )
