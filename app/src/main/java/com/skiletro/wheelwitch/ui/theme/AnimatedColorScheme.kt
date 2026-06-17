package com.skiletro.wheelwitch.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Returns a [ColorScheme] that smoothly interpolates from the previous value to
 * [target] over [durationMs] milliseconds, animating each color slot
 * independently.
 *
 * Why this exists: a `Crossfade(targetState = colorScheme) { scheme -> MaterialTheme(...) }`
 * approach re-creates the [MaterialTheme] content tree on every target change,
 * which resets [androidx.compose.runtime.remember] state in descendants
 * (e.g. the Settings overlay's `showSettings` flips back to `false`, bouncing
 * the user to the home menu). By animating each color value and passing a
 * single, stable [ColorScheme] to a single [MaterialTheme] call, the content
 * tree shape is preserved and `remember` state survives the transition.
 */
@Composable
fun rememberAnimatedColorScheme(
    target: ColorScheme,
    durationMs: Int = 300,
): ColorScheme {
    val spec = tween<Color>(durationMillis = durationMs)

    val primary by animateColorAsState(target.primary, spec, label = "c_primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "c_on_primary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "c_primary_container")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "c_on_primary_container")
    val inversePrimary by animateColorAsState(target.inversePrimary, spec, label = "c_inverse_primary")
    val secondary by animateColorAsState(target.secondary, spec, label = "c_secondary")
    val onSecondary by animateColorAsState(target.onSecondary, spec, label = "c_on_secondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "c_secondary_container")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, spec, label = "c_on_secondary_container")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "c_tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, spec, label = "c_on_tertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "c_tertiary_container")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, spec, label = "c_on_tertiary_container")
    val background by animateColorAsState(target.background, spec, label = "c_background")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "c_on_background")
    val surface by animateColorAsState(target.surface, spec, label = "c_surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "c_on_surface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "c_surface_variant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "c_on_surface_variant")
    val surfaceTint by animateColorAsState(target.surfaceTint, spec, label = "c_surface_tint")
    val inverseSurface by animateColorAsState(target.inverseSurface, spec, label = "c_inverse_surface")
    val inverseOnSurface by animateColorAsState(target.inverseOnSurface, spec, label = "c_inverse_on_surface")
    val error by animateColorAsState(target.error, spec, label = "c_error")
    val onError by animateColorAsState(target.onError, spec, label = "c_on_error")
    val errorContainer by animateColorAsState(target.errorContainer, spec, label = "c_error_container")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, spec, label = "c_on_error_container")
    val outline by animateColorAsState(target.outline, spec, label = "c_outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "c_outline_variant")
    val scrim by animateColorAsState(target.scrim, spec, label = "c_scrim")
    val surfaceBright by animateColorAsState(target.surfaceBright, spec, label = "c_surface_bright")
    val surfaceDim by animateColorAsState(target.surfaceDim, spec, label = "c_surface_dim")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, spec, label = "c_surface_container")
    val surfaceContainerHigh by animateColorAsState(target.surfaceContainerHigh, spec, label = "c_surface_container_high")
    val surfaceContainerHighest by animateColorAsState(target.surfaceContainerHighest, spec, label = "c_surface_container_highest")
    val surfaceContainerLow by animateColorAsState(target.surfaceContainerLow, spec, label = "c_surface_container_low")
    val surfaceContainerLowest by animateColorAsState(target.surfaceContainerLowest, spec, label = "c_surface_container_lowest")

    val isDark = target.background.luminance() < 0.5f
    return if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceDim = surfaceDim,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            inversePrimary = inversePrimary,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondaryContainer,
            onSecondaryContainer = onSecondaryContainer,
            tertiary = tertiary,
            onTertiary = onTertiary,
            tertiaryContainer = tertiaryContainer,
            onTertiaryContainer = onTertiaryContainer,
            background = background,
            onBackground = onBackground,
            surface = surface,
            onSurface = onSurface,
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = onSurfaceVariant,
            surfaceTint = surfaceTint,
            inverseSurface = inverseSurface,
            inverseOnSurface = inverseOnSurface,
            error = error,
            onError = onError,
            errorContainer = errorContainer,
            onErrorContainer = onErrorContainer,
            outline = outline,
            outlineVariant = outlineVariant,
            scrim = scrim,
            surfaceBright = surfaceBright,
            surfaceDim = surfaceDim,
            surfaceContainer = surfaceContainer,
            surfaceContainerHigh = surfaceContainerHigh,
            surfaceContainerHighest = surfaceContainerHighest,
            surfaceContainerLow = surfaceContainerLow,
            surfaceContainerLowest = surfaceContainerLowest,
        )
    }
}
