package com.fadynemer.cutime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fadynemer.cutime.navigation.AppNavigation
import com.fadynemer.cutime.ui.theme.CutTimeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CutTimeTheme {
                AppNavigation()
            }
        }
    }
}