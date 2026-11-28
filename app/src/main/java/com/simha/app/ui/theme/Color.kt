package com.simha.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val DuskPage = Color(0xFFF7F2EA)
val DuskSurface = Color(0xFFFFFFFF)
val DuskFill = Color(0xFFE7E4EF)
val DuskBorder = Color(0xFFE2D8C8)
val DuskText = Color(0xFF1E2546)
val DuskText2 = Color(0xFF5E5A6E)
val DuskPrimary = Color(0xFF28356B)
val DuskGold = Color(0xFFE8A33D)
val SuccessGreen = Color(0xFF2F6B4F)
val EmergencyRed = Color(0xFFB3261E)

val NightPage = Color(0xFF11121C)
val NightSurface = Color(0xFF1A1C2A)
val NightFill = Color(0xFF26293D)
val NightBorder = Color(0xFF2E3146)
val NightText = Color(0xFFEDEBF5)
val NightText2 = Color(0xFFA9A6BD)
val NightPrimary = Color(0xFFA9B4EE)
val NightOnPrimary = Color(0xFF141B44)
val NightGold = Color(0xFFEBAE4E)
val NightSuccess = Color(0xFF7CC9A0)

@Immutable
data class SimhaColors(
    val page: Color,
    val surface: Color,
    val fill: Color,
    val border: Color,
    val text: Color,
    val text2: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val success: Color,
    val emergency: Color,
)

val LightSimha = SimhaColors(DuskPage, DuskSurface, DuskFill, DuskBorder, DuskText, DuskText2, DuskPrimary, Color.White, DuskGold, SuccessGreen, EmergencyRed)
val DarkSimha = SimhaColors(NightPage, NightSurface, NightFill, NightBorder, NightText, NightText2, NightPrimary, NightOnPrimary, NightGold, NightSuccess, EmergencyRed)
