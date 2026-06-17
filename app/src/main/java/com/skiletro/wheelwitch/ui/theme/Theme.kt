package com.skiletro.wheelwitch.ui.theme

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import android.app.Activity

private val DarkColorScheme = darkColorScheme(
    primary = Red80,
    onPrimary = OnRed80,
    secondary = RedGrey80,
    tertiary = LightRed80
)

private val LightColorScheme = lightColorScheme(
    primary = Red40,
    onPrimary = OnRed40,
    secondary = RedGrey40,
    tertiary = LightRed40
)

enum class ThemeMode { Light, Dark, System }

@Composable
fun WheelWitchTheme(
    themeMode: ThemeMode = ThemeMode.System,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
        ThemeMode.System -> isSystemInDarkTheme()
    }
    val colorScheme = if (dynamicColor) {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    val statusColors = if (darkTheme) DarkStatusColors else LightStatusColors

    val context = LocalContext.current
    SideEffect {
        (context as? Activity)?.window?.decorView?.setBackgroundColor(
            colorScheme.background.toArgb()
        )
    }

    CompositionLocalProvider(LocalStatusColors provides statusColors) {
        Crossfade(
            targetState = colorScheme,
            animationSpec = tween(durationMillis = 400),
            label = "theme_colors",
        ) { scheme ->
            MaterialTheme(
                colorScheme = scheme,
                typography = Typography,
                content = content
            )
        }
    }
}
