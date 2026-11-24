package com.simha.app.ui.home

import android.util.Log
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.simha.app.R
import com.simha.app.ui.animMs
import com.simha.app.ui.home.orb.OrbGlView
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class OrbState { Idle, Listening, Speaking, Thinking }

private val Rose = Color(0xFF7E2F3B)
private val Blush = Color(0xFFE9A2AC)
private val Peach = Color(0xFFF4C7B0)
private val Gold = Color(0xFFE2B26A)
private val Cream = Color(0xFFFFF1E2)

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
        if (still) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "level",
    )
    val listenA by animateFloatAsState(if (state == OrbState.Listening) 1f else 0f, tween(if (still) 0 else 300), label = "listenA")
    val speakA by animateFloatAsState(if (state == OrbState.Speaking) 1f else 0f, tween(if (still) 0 else 300), label = "speakA")
    val dimS = animateFloatAsState(if (state == OrbState.Thinking) 1f else 0f, tween(if (still) 0 else 400), label = "dimA")
    val dimA by dimS
    val phase = remember { mutableFloatStateOf(3f) }
    val clock = remember { mutableFloatStateOf(0f) }
    val st by rememberUpdatedState(state)
    var glFailed by remember { mutableStateOf(false) }

    LaunchedEffect(pressed) { if (pressed) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }

    LaunchedEffect(still) {
        if (still) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            var last = 0L
            var sp = 0.45f
            androidx.compose.runtime.withFrameNanos { last = it }
            while (true) {
                androidx.compose.runtime.withFrameNanos { n ->
                    val dt = ((n - last) / 1_000_000_000f).coerceIn(0f, 0.05f)
                    last = n
                    val target = when (st) {
                        OrbState.Idle -> 0.45f
                        OrbState.Listening -> 0.7f
                        OrbState.Speaking -> 0.8f + 2.4f * lv.value
                        OrbState.Thinking -> 1.2f
                    }
                    sp += (target - sp) * minOf(1f, dt * 4f)
                    phase.floatValue += dt * sp
                    clock.floatValue += dt
                }
            }
        }
    }

    fun breathe() = if (still) 0f else 0.5f - 0.5f * cos(clock.floatValue * 2f * PI.toFloat() / 4f)
    fun grow() = 1f + 0.025f * breathe() + 0.06f * lv.value * listenA + 0.05f * lv.value * speakA
    fun wobble() = if (still) 0f else 0.035f * lv.value * speakA * (0.6f * sin(clock.floatValue * 6.1f) + 0.4f * sin(clock.floatValue * 9.7f + 1f))

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
        val m = size.minDimension
        val rad = m * 0.34f
        val cx = size.width / 2f
        val cy = size.height / 2f - m * 0.06f
        val c = Offset(cx, cy)
        val l = lv.value
        val sc = grow()
        val wob = wobble()
        val sx = sc * (1f + wob)
        val sy = sc * (1f - wob * 0.8f)

        scale(1f - 0.4f * (sc - 1f), 0.22f, Offset(cx, cy + rad + m * 0.06f)) {
            drawCircle(
                Brush.radialGradient(
                    listOf(Rose.copy(alpha = 0.38f), Color.Transparent),
                    Offset(cx, cy + rad + m * 0.06f),
                    rad * 0.9f,
                ),
                rad * 0.9f,
                Offset(cx, cy + rad + m * 0.06f),
            )
        }

        if (listenA > 0.01f) {
            val gr = rad * 1.35f
            drawCircle(
                Brush.radialGradient(
                    0.74f to Blush.copy(alpha = 0.45f * l * listenA),
                    1f to Color.Transparent,
                    center = c,
                    radius = gr,
                ),
                gr,
                c,
            )
            drawCircle(
                Blush.copy(alpha = (0.18f + 0.45f * l) * listenA),
                rad * (1.08f + 0.1f * l),
                c,
                style = Stroke(3.dp.toPx()),
            )
        }

        if (glFailed) scale(sx, sy, c) {
            run {
                val p = phase.floatValue
                val clip = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(c, rad))
                }
                clipPath(clip) {
                    drawCircle(
                        Brush.linearGradient(listOf(Peach, Blush), Offset(cx - rad, cy - rad), Offset(cx + rad, cy + rad)),
                        rad,
                        c,
                    )
                    val cols = listOf(Rose, Peach, Gold, Blush, Cream)
                    val orb = floatArrayOf(0.45f, 0.55f, 0.4f, 0.6f, 0.35f)
                    val spd = floatArrayOf(0.9f, -0.7f, 1.3f, -1.1f, 0.6f)
                    val br = floatArrayOf(0.8f, 0.7f, 0.65f, 0.75f, 0.55f)
                    for (i in 0 until 5) {
                        val a = p * spd[i] + i * 1.3f
                        val pos = Offset(cx + rad * orb[i] * cos(a), cy + rad * orb[i] * sin(a * 0.9f + i))
                        val r = rad * br[i] * (1f + 0.15f * l)
                        drawCircle(
                            Brush.radialGradient(listOf(cols[i].copy(alpha = if (i == 0) 0.75f else 0.85f), Color.Transparent), pos, r),
                            r,
                            pos,
                        )
                    }
                    drawCircle(
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = 0.28f),
                            0.5f to Color.Transparent,
                            1f to Rose.copy(alpha = 0.5f),
                            center = Offset(cx - rad * 0.35f, cy - rad * 0.4f),
                            radius = rad * 1.7f,
                        ),
                        rad,
                        c,
                    )
                    drawCircle(
                        Brush.radialGradient(0.72f to Color.Transparent, 1f to Rose.copy(alpha = 0.4f), center = c, radius = rad),
                        rad,
                        c,
                    )
                    if (dimA > 0f) drawCircle(Color(0xFF3A1A20).copy(alpha = 0.18f * dimA), rad, c)
                }
                val sp = Offset(cx - rad * 0.38f, cy - rad * 0.48f)
                withTransform({
                    rotate(-35f, sp)
                    scale(1f, 0.5f, sp)
                }) {
                    drawCircle(
                        Brush.radialGradient(listOf(Color.White.copy(alpha = 0.75f), Color.Transparent), sp, rad * 0.3f),
                        rad * 0.3f,
                        sp,
                    )
                }
            }
        }
    }
    if (!glFailed) AndroidView(
        factory = { c ->
            OrbGlView(c) { e ->
                Log.e("SimhaOrb", "GL init failed, using canvas", e)
                glFailed = true
            }.also {
                it.phase = { phase.floatValue }
                it.level = { lv.value }
                it.dim = { dimS.value }
                it.bind(owner)
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val w = wobble()
                val g = grow()
                scaleX = g * (1f + w)
                scaleY = g * (1f - w * 0.8f)
                transformOrigin = TransformOrigin(0.5f, 0.44f)
            },
    )
    }
}
