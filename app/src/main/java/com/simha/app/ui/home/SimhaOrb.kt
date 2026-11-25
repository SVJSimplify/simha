package com.simha.app.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.simha.app.R
import com.simha.app.ui.animMs
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState { Idle, Listening, Speaking, Thinking }

private val Base = Color(0xFFFFF3DC)
private val Amber = Color(0xFFE39A3D)
private val Honey = Color(0xFFFBE3B5)
private val Apricot = Color(0xFFF2B661)
private val Butter = Color(0xFFFFEBC8)
private val Cream = Color(0xFFFFF8EA)

private class Cloud(
    val color: Color,
    val alpha: Float,
    val orbit: Float,
    val fx: Float,
    val fy: Float,
    val ox: Float,
    val oy: Float,
    val size: Float,
)

private val Clouds = listOf(
    Cloud(Apricot, 0.85f, 0.38f, 1f, 0.7f, 0f, 1f, 0.75f),
    Cloud(Cream, 0.95f, 0.42f, -0.8f, 1f, 2.1f, 0.3f, 0.7f),
    Cloud(Amber, 0.5f, 0.36f, 0.6f, -0.9f, 4.2f, 2.2f, 0.65f),
    Cloud(Honey, 0.9f, 0.4f, -1.1f, -0.6f, 1.2f, 3.9f, 0.8f),
    Cloud(Butter, 0.7f, 0.3f, 0.9f, 1.2f, 3.3f, 5.1f, 0.6f),
)

@Composable
fun SimhaOrb(state: OrbState, onTap: () -> Unit, modifier: Modifier = Modifier, level: Float = 0f) {
    val still = animMs() == 0
    val desc = stringResource(R.string.r3_orb_caption_tap)
    val haptic = LocalHapticFeedback.current
    val owner = LocalLifecycleOwner.current
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val press by animateFloatAsState(
        if (pressed) 0.95f else 1f,
        if (still) snap() else spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    val lv = animateFloatAsState(
        if (still) 0f else level.coerceIn(0f, 1f),
        if (still) snap() else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "level",
    )
    val listenA by animateFloatAsState(if (state == OrbState.Listening) 1f else 0f, tween(if (still) 0 else 300), label = "listenA")
    val speakA by animateFloatAsState(if (state == OrbState.Speaking) 1f else 0f, tween(if (still) 0 else 300), label = "speakA")
    val phase = remember { mutableFloatStateOf(3f) }
    val rot = remember { mutableFloatStateOf(0f) }
    val clock = remember { mutableFloatStateOf(0f) }
    val st by rememberUpdatedState(state)

    LaunchedEffect(pressed) { if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = 0L
            var sp = 0.52f
            var rs = 0f
            withFrameNanos { last = it }
            while (true) {
                withFrameNanos { n ->
                    val dt = ((n - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    last = n
                    val target = when (st) {
                        OrbState.Idle -> 0.52f
                        OrbState.Listening -> 0.8f
                        OrbState.Speaking -> 0.8f + 2.4f * lv.value
                        OrbState.Thinking -> 0.3f
                    }
                    val rt = if (st == OrbState.Thinking) 20f else 0f
                    val k = minOf(1f, dt * 4f)
                    sp += (target - sp) * k
                    rs += (rt - rs) * k
                    phase.floatValue += dt * sp
                    rot.floatValue = (rot.floatValue + dt * rs) % 360f
                    clock.floatValue += dt
                }
            }
        }
    }

    Box(
        modifier
            .size(200.dp)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .semantics(mergeDescendants = true) { contentDescription = desc }
            .clickable(interactionSource = src, indication = null, role = Role.Button, onClick = onTap),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val breath = if (still) 0f else 0.5f - 0.5f * cos(clock.floatValue * 2f * PI.toFloat() / 6f)
            val grow = 1f + 0.02f * breath + 0.08f * lv.value * listenA + 0.06f * lv.value * speakA
            val rad = size.minDimension * 0.45f * grow / 1.08f
            val c = Offset(size.width / 2f, size.height / 2f)
            val p = phase.floatValue
            drawCircle(Base, rad, c)
            clipPath(Path().apply { addOval(Rect(c, rad)) }) {
                rotate(rot.floatValue, c) {
                    Clouds.forEach { k ->
                        val pos = Offset(
                            c.x + rad * k.orbit * cos(p * k.fx + k.ox),
                            c.y + rad * k.orbit * sin(p * k.fy + k.oy),
                        )
                        val r = rad * k.size
                        drawCircle(
                            Brush.radialGradient(listOf(k.color.copy(alpha = k.alpha), k.color.copy(alpha = 0f)), pos, r),
                            r,
                            pos,
                        )
                    }
                }
            }
        }
    }
}
