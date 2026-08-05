package com.fadynemer.cutime.components

import com.fadynemer.cutime.R

import androidx.compose.ui.res.stringResource

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fadynemer.cutime.ui.theme.CutTimeNavy

import com.fadynemer.cutime.ui.theme.CutTimeRed
@Composable
fun NotificationIconButton(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        BadgedBox(
            badge = {
                if (unreadCount > 0) {
                    Badge(containerColor = CutTimeRed) {
                        Text(
                            if (unreadCount > 99) {
                                "99+"
                            } else {
                                unreadCount.toString()
                            }
                        )
                    }
                }
            }
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = stringResource(R.string.content_description_notifications),
                tint = CutTimeNavy
            )
        }
    }
}
