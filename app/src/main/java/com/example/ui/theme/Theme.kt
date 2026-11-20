package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val SmsDarkColorScheme = darkColorScheme(
    primary = EmeraldVibrant,
    onPrimary = ObsidianBlack,
    primaryContainer = EmeraldDarkContainer,
    onPrimaryContainer = MintAccent,
    secondary = EmeraldPrimary,
    onSecondary = ObsidianBlack,
    secondaryContainer = ObsidianElevated,
    onSecondaryContainer = TextOnDarkPrimary,
    tertiary = MintAccent,
    onTertiary = ObsidianBlack,
    background = ObsidianBlack,
    onBackground = TextOnDarkPrimary,
    surface = ObsidianSurface,
    onSurface = TextOnDarkPrimary,
    surfaceVariant = ObsidianCard,
    onSurfaceVariant = TextOnDarkSecondary,
    surfaceContainer = ObsidianCard,
    surfaceContainerHigh = ObsidianElevated,
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1B2B23),
    error = CallRejectRed,
    onError = Color.White
)

private val SmsLightColorScheme = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldLightContainer,
    onPrimaryContainer = Color(0xFF003919),
    secondary = EmeraldPrimary,
    onSecondary = Color.White,
    secondaryContainer = CrispLightCard,
    onSecondaryContainer = TextOnLightPrimary,
    tertiary = EmeraldVibrant,
    onTertiary = ObsidianBlack,
    background = CrispLightBg,
    onBackground = TextOnLightPrimary,
    surface = CrispLightSurface,
    onSurface = TextOnLightPrimary,
    surfaceVariant = CrispLightCard,
    onSurfaceVariant = TextOnLightSecondary,
    surfaceContainer = CrispLightSurface,
    surfaceContainerHigh = CrispLightCard,
    outline = CrispLightBorder,
    outlineVariant = Color(0xFFE0EDE5),
    error = CallRejectRed,
    onError = Color.White
)

val SmsChatShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SmsChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) SmsDarkColorScheme else SmsLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = SmsChatTypography,
        shapes = SmsChatShapes,
        content = content
    )
}
