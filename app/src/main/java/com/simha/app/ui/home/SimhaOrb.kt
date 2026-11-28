package com.simha.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.simha.app.R
import com.simha.app.ui.animMs

enum class OrbState { Idle, Listening, Speaking, Thinking }

@Composable
fun SimhaOrb(state: OrbState, onTap: () -> Unit, modifier: Modifier = Modifier, level: Float = 0f) {
    val still = animMs() == 0
    val desc = stringResource(R.string.r3_orb_caption_tap)
    val haptic = LocalHapticFeedback.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        if (still) snap() else spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val lv by animateFloatAsState(
        if (still) 0f else level.coerceIn(0f, 1f),
        if (still) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "level",
    )
    val speed by animateFloatAsState(
        when (state) {
            OrbState.Idle -> 1f
            OrbState.Listening -> 1.6f
            OrbState.Speaking -> 1.8f
            OrbState.Thinking -> 0.6f
        },
        tween(if (still) 0 else 500),
        label = "speed",
    )
    val voice = if (state == OrbState.Listening || state == OrbState.Speaking) lv else 0f
    val turn by rememberInfiniteTransition(label = "turn").animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot",
    )
    val comp by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.simha_orb))
    val progress by animateLottieCompositionAsState(
        comp,
        iterations = LottieConstants.IterateForever,
        isPlaying = !still,
        speed = speed,
    )

    LaunchedEffect(pressed) { if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    Box(
        modifier
            .size(200.dp)
            .graphicsLayer {
                val g = press * (1f + 0.08f * voice)
                scaleX = g
                scaleY = g
                rotationZ = if (state == OrbState.Thinking && !still) turn else 0f
            }
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onTap),
    ) {
        LottieAnimation(comp, { if (still) 0.3f else progress }, Modifier.fillMaxSize())
    }
}
