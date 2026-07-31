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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.fadynemer.cutime.R
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
    val homeLabel = stringResource(R.string.nav_home)
    val appointmentsLabel =
        stringResource(R.string.nav_appointments)
    val profileLabel = stringResource(R.string.nav_profile)
    NavigationBar {
        NavigationBarItem(
            selected =
                selectedDestination == CustomerDestination.HOME,
            onClick = onHomeSelected,
            icon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = homeLabel
                )
            },
            label = {
                Text(homeLabel, maxLines = 1)
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
                    contentDescription = appointmentsLabel
                )
            },
            label = {
                Text(
                    appointmentsLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    contentDescription = profileLabel
                )
            },
            label = {
                Text(profileLabel, maxLines = 1)
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
