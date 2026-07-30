package com.fadynemer.cutime.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.fadynemer.cutime.model.NotificationType
import com.fadynemer.cutime.model.UserNotification
import com.fadynemer.cutime.ui.theme.CutTimeNavy
import com.fadynemer.cutime.ui.theme.CutTimeRed
import com.fadynemer.cutime.ui.theme.CutTimeTextSecondary
import com.fadynemer.cutime.util.NotificationTimeFormatter
import com.fadynemer.cutime.viewmodel.NotificationCenterViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterScreen(
    isBarberMode: Boolean,
    onBack: () -> Unit,
    onOpenSettings: (Boolean) -> Unit,
    onNotificationSelected: (UserNotification, Boolean) -> Unit,
    notificationViewModel:
        NotificationCenterViewModel = viewModel()
) {
    val uiState = notificationViewModel.uiState

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Notifications",
                            color = CutTimeNavy,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (uiState.unreadCount > 0) {
                            Spacer(modifier = Modifier.size(8.dp))
                            Badge {
                                Text(uiState.unreadCount.toString())
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = CutTimeNavy
                        )
                    }
                },
                actions = {
                    if (uiState.unreadCount > 0) {
                        TextButton(
                            onClick =
                                notificationViewModel::markAllRead,
                            enabled = !uiState.isMarkingAllRead
                        ) {
                            Text("Read all")
                        }
                    }
                    IconButton(
                        onClick = {
                            onOpenSettings(isBarberMode)
                        }
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription =
                                "Notification settings",
                            tint = CutTimeNavy
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CutTimeNavy)
                }
            }

            uiState.errorMessage != null &&
                uiState.notifications.isEmpty() -> {
                NotificationErrorState(
                    message = uiState.errorMessage,
                    onRetry = notificationViewModel::retry,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            uiState.isEmpty -> {
                EmptyNotifications(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }

            else -> {
                NotificationList(
                    notifications = uiState.notifications,
                    updatingNotificationId =
                        uiState.updatingNotificationId,
                    errorMessage = uiState.errorMessage,
                    onSelected = { notification ->
                        notificationViewModel.markRead(
                            notification
                        ) {
                            onNotificationSelected(
                                notification,
                                isBarberMode
                            )
                        }
                    },
                    onDelete =
                        notificationViewModel::delete,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun NotificationList(
    notifications: List<UserNotification>,
    updatingNotificationId: String?,
    errorMessage: String?,
    onSelected: (UserNotification) -> Unit,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups =
        notifications.groupBy {
            NotificationTimeFormatter.dayGroup(
                it.createdAtMillis
            )
        }
    val groupOrder =
        listOf("Today", "Yesterday", "This week", "Earlier", "Recent")

    LazyColumn(
        modifier = modifier,
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        errorMessage?.let { message ->
            item {
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        groupOrder.forEach { group ->
            val groupNotifications = groups[group].orEmpty()
            if (groupNotifications.isNotEmpty()) {
                item(key = "header_$group") {
                    Text(
                        text = group,
                        color = CutTimeTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(
                            top = 8.dp,
                            bottom = 2.dp
                        )
                    )
                }
                items(
                    items = groupNotifications,
                    key = UserNotification::id
                ) { notification ->
                    NotificationCard(
                        notification = notification,
                        isUpdating =
                            updatingNotificationId ==
                                notification.id,
                        onSelected = {
                            onSelected(notification)
                        },
                        onDelete = {
                            onDelete(notification.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(
    notification: UserNotification,
    isUpdating: Boolean,
    onSelected: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = !isUpdating,
                onClick = onSelected
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor =
                if (notification.isRead) {
                    MaterialTheme.colorScheme.surface
                } else {
                    CutTimeNavy.copy(alpha = 0.07f)
                }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation =
                if (notification.isRead) 1.dp else 3.dp
        )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = CutTimeNavy.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector =
                            notificationIcon(notification.type),
                        contentDescription = null,
                        tint = CutTimeNavy,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        color = CutTimeNavy,
                        fontWeight =
                            if (notification.isRead) {
                                FontWeight.Medium
                            } else {
                                FontWeight.Bold
                            },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (!notification.isRead) {
                        Surface(
                            modifier = Modifier.size(8.dp),
                            shape = CircleShape,
                            color = CutTimeRed
                        ) {}
                    }
                }
                Spacer(modifier = Modifier.size(3.dp))
                Text(
                    text = notification.message,
                    color = CutTimeTextSecondary,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.size(5.dp))
                Text(
                    text =
                        NotificationTimeFormatter.format(
                            notification.createdAtMillis
                        ),
                    color = CutTimeTextSecondary,
                    fontSize = 12.sp
                )
            }
            IconButton(
                onClick = onDelete,
                enabled = !isUpdating
            ) {
                if (isUpdating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete notification",
                        tint = CutTimeTextSecondary
                    )
                }
            }
        }
    }
}

private fun notificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.APPOINTMENT_BOOKED ->
            Icons.Default.CalendarMonth
        NotificationType.APPOINTMENT_CANCELLED ->
            Icons.Default.Cancel
        NotificationType.APPOINTMENT_RESCHEDULED ->
            Icons.Default.Schedule
        NotificationType.APPOINTMENT_COMPLETED ->
            Icons.Default.CheckCircle
        NotificationType.APPOINTMENT_REMINDER ->
            Icons.Default.Notifications
        NotificationType.REVIEW_REQUEST ->
            Icons.Default.RateReview
        NotificationType.GENERAL ->
            Icons.Default.Notifications
    }
}

@Composable
private fun EmptyNotifications(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Notifications,
            contentDescription = null,
            tint = CutTimeNavy,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = "No notifications yet",
            color = CutTimeNavy,
            fontSize = 21.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.size(6.dp))
        Text(
            text =
                "Booking updates and appointment reminders will appear here.",
            color = CutTimeTextSecondary
        )
    }
}

@Composable
private fun NotificationErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.size(14.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = CutTimeNavy
            )
        ) {
            Text("Retry")
        }
    }
}
