package com.simha.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simha.app.R

val SimhaRadius = 24.dp

val LocalSimha = staticCompositionLocalOf { LightSimha }

@OptIn(ExperimentalTextApi::class)
private fun baloo(res: Int) = FontFamily(
    listOf(400, 500, 600).map { w -> Font(res, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) },
)

private val Baloo = baloo(R.font.baloo2)
private val BalooTe = baloo(R.font.baloo_tammudu2)
private val BalooKn = baloo(R.font.baloo_tamma2)
private val BalooTa = baloo(R.font.baloo_thambi2)

fun simhaFont(lang: String): FontFamily = when (lang.take(2)) {
    "te" -> BalooTe
    "kn" -> BalooKn
    "ta" -> BalooTa
    else -> Baloo
}

private fun scheme(c: SimhaColors, dark: Boolean) = if (dark) {
    darkColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.fill,
        onPrimaryContainer = c.text,
        secondary = c.text2,
        onSecondary = c.page,
        secondaryContainer = c.fill,
        onSecondaryContainer = c.text,
        tertiary = c.success,
        onTertiary = c.page,
        background = c.page,
        onBackground = c.text,
        surface = c.surface,
        onSurface = c.text,
        surfaceVariant = c.fill,
        onSurfaceVariant = c.text2,
        outline = c.border,
        outlineVariant = c.border,
        error = c.emergency,
        onError = Color.White,
    )
} else {
    lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        primaryContainer = c.fill,
        onPrimaryContainer = c.text,
        secondary = c.text2,
        onSecondary = c.surface,
        secondaryContainer = c.fill,
        onSecondaryContainer = c.text,
        tertiary = c.success,
        onTertiary = Color.White,
        background = c.page,
        onBackground = c.text,
        surface = c.surface,
        onSurface = c.text,
        surfaceVariant = c.fill,
        onSurfaceVariant = c.text2,
        outline = c.border,
        outlineVariant = c.border,
        error = c.emergency,
        onError = Color.White,
    )
}

private fun typography(f: FontFamily) = Typography(
    titleLarge = TextStyle(fontFamily = f, fontFeatureSettings = "tnum", fontSize = 28.sp, fontWeight = FontWeight.Medium, lineHeight = 36.sp),
    titleMedium = TextStyle(fontFamily = f, fontFeatureSettings = "tnum", fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 30.sp),
    bodyLarge = TextStyle(fontFamily = f, fontFeatureSettings = "tnum", fontSize = 20.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontFamily = f, fontFeatureSettings = "tnum", fontSize = 20.sp, fontWeight = FontWeight.Normal, lineHeight = 28.sp),
    labelLarge = TextStyle(fontFamily = f, fontFeatureSettings = "tnum", fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp),
    bodySmall = TextStyle(fontFamily = f, fontFeatureSettings = "tnum", fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
    labelSmall = TextStyle(fontFamily = f, fontFeatureSettings = "tnum", fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimhaTheme(dark: Boolean, lang: String = "en", content: @Composable () -> Unit) {
    val c = if (dark) DarkSimha else LightSimha
    val type = remember(lang) { typography(simhaFont(lang)) }
    CompositionLocalProvider(
        LocalSimha provides c,
        LocalRippleConfiguration provides RippleConfiguration(color = c.primary, rippleAlpha = RippleAlpha(0.1f, 0.1f, 0.1f, 0.1f)),
    ) {
        MaterialTheme(
            colorScheme = scheme(c, dark),
            typography = type,
            content = content,
        )
    }
}
