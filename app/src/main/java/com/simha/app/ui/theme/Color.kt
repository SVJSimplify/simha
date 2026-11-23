package com.simha.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

val RosePage = Color(0xFFFBF0EE)
val RoseSurface = Color(0xFFFFFFFF)
val RoseFill = Color(0xFFF6E3DF)
val RoseBorder = Color(0xFFEBD5D1)
val RoseText = Color(0xFF2B1D1F)
val RoseText2 = Color(0xFF634B4E)
val RosePrimary = Color(0xFF7E2F3B)
val ClayAccent = Color(0xFFE2B26A)
val SuccessGreen = Color(0xFF2F6B4F)
val EmergencyRed = Color(0xFFB3261E)

val NightPage = Color(0xFF1E1517)
val NightSurface = Color(0xFF2A1F21)
val NightFill = Color(0xFF36272A)
val NightBorder = Color(0xFF4A3639)
val NightText = Color(0xFFF6E9E7)
val NightText2 = Color(0xFFD2BDBA)
val NightPrimary = Color(0xFFE9A2AC)
val NightOnPrimary = Color(0xFF2B1D1F)
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

val LightSimha = SimhaColors(RosePage, RoseSurface, RoseFill, RoseBorder, RoseText, RoseText2, RosePrimary, Color.White, ClayAccent, SuccessGreen, EmergencyRed)
val DarkSimha = SimhaColors(NightPage, NightSurface, NightFill, NightBorder, NightText, NightText2, NightPrimary, NightOnPrimary, ClayAccent, NightSuccess, EmergencyRed)
