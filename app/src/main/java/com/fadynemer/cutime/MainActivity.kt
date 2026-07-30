package com.fadynemer.cutime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fadynemer.cutime.navigation.AppNavigation
import com.fadynemer.cutime.notifications.NotificationChannels
import com.fadynemer.cutime.notifications.NotificationRegistrationManager
import com.fadynemer.cutime.notifications.SystemNotificationPublisher
import com.fadynemer.cutime.ui.theme.CutTimeTheme

class MainActivity : ComponentActivity() {
    private var notificationRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationChannels.create(this)
        notificationRoute =
            intent.getStringExtra(
                SystemNotificationPublisher.EXTRA_ROUTE
            )
        NotificationRegistrationManager.sync(
            applicationContext
        )

        setContent {
            CutTimeTheme {
                AppNavigation(
                    externalRoute = notificationRoute,
                    onExternalRouteConsumed = {
                        notificationRoute = null
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationRoute =
            intent.getStringExtra(
                SystemNotificationPublisher.EXTRA_ROUTE
            )
    }
}
