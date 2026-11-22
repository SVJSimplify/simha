package com.simha.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightScheme = lightColorScheme(
    primary = Slate,
    onPrimary = Sand,
    primaryContainer = DeepSand,
    onPrimaryContainer = Slate,
    secondary = MutedSlate,
    onSecondary = Sand,
    secondaryContainer = DeepSand,
    onSecondaryContainer = Slate,
    tertiary = SuccessGreen,
    onTertiary = Color.White,
    background = Sand,
    onBackground = Slate,
    surface = Cream,
    onSurface = Slate,
    surfaceVariant = DeepSand,
    onSurfaceVariant = MutedSlate,
    outline = BorderGrey,
    error = EmergencyRed,
    onError = Color.White,
)

private val DarkScheme = darkColorScheme(
    primary = Sand,
    onPrimary = DarkBg,
    primaryContainer = DarkSurfaceAlt,
    onPrimaryContainer = Sand,
    secondary = DarkMuted,
    onSecondary = DarkBg,
    secondaryContainer = DarkSurfaceAlt,
    onSecondaryContainer = Sand,
    tertiary = DarkSuccess,
    onTertiary = DarkBg,
    background = DarkBg,
    onBackground = Sand,
    surface = DarkSurface,
    onSurface = Sand,
    surfaceVariant = DarkSurfaceAlt,
    onSurfaceVariant = DarkMuted,
    outline = BorderGrey,
    error = EmergencyRed,
    onError = Color.White,
)

private val SimhaTypography = Typography(
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Medium, lineHeight = 40.sp),
    bodyLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, lineHeight = 30.sp),
    bodyMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Normal, lineHeight = 30.sp),
    labelLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp),
    bodySmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
    labelSmall = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 24.sp),
)

@Composable
fun SimhaTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = SimhaTypography,
        content = content,
    )
}
