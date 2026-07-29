package com.fadynemer.cutime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = CutTimeNavy,
    onPrimary = CutTimeWhite,

    secondary = CutTimeRed,
    onSecondary = CutTimeWhite,

    background = CutTimeBackground,
    onBackground = CutTimeTextPrimary,

    surface = CutTimeSurface,
    onSurface = CutTimeTextPrimary,

    error = CutTimeError,
    onError = CutTimeWhite
)

private val DarkColorScheme = darkColorScheme(
    primary = CutTimeRed,
    onPrimary = CutTimeWhite,

    secondary = CutTimeNavy,
    onSecondary = CutTimeWhite,

    background = CutTimeNavy,
    onBackground = CutTimeWhite,

    surface = CutTimeTextPrimary,
    onSurface = CutTimeWhite,

    error = CutTimeError,
    onError = CutTimeWhite
)

@Composable
fun CutTimeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme =
        if (darkTheme) DarkColorScheme
        else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}