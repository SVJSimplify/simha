package com.simha.app.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.simha.app.R
import com.simha.app.ui.animMs
import com.simha.app.ui.pressClick
import com.simha.app.ui.theme.LocalSimha

enum class OrbState { Idle, Listening, Speaking, Thinking }

@Composable
fun SimhaOrb(state: OrbState, onTap: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalSimha.current
    val still = animMs() == 0
    val t = rememberInfiniteTransition(label = "orb")
    val breathe by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val pulse by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "ripple")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "spin")
    val desc = stringResource(
        when (state) {
            OrbState.Idle -> R.string.r3_orb_idle
            OrbState.Listening -> R.string.r3_orb_listening
            OrbState.Speaking -> R.string.r3_orb_speaking
            OrbState.Thinking -> R.string.r3_orb_thinking
        }
    )
    Canvas(
        modifier
            .defaultMinSize(160.dp, 160.dp)
            .aspectRatio(1f)
            .pressClick(onTap)
            .semantics(mergeDescendants = true) { contentDescription = desc },
    ) {
        val mid = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension * 0.3f
        val scale = if (still) 1f else when (state) {
            OrbState.Idle -> 0.96f + 0.08f * breathe
            OrbState.Listening -> 1f + 0.03f * breathe
            OrbState.Speaking -> 1f + 0.09f * pulse
            OrbState.Thinking -> 1f
        }
        val glow = if (still) 0.25f else when (state) {
            OrbState.Idle -> 0.15f + 0.2f * breathe
            OrbState.Speaking -> 0.2f + 0.25f * pulse
            else -> 0.25f
        }
        val r = base * scale
        drawCircle(c.accent.copy(alpha = glow), r * 1.35f, mid)
        if (!still && state == OrbState.Listening) {
            for (i in 0 until 3) {
                val p = (ripple + i / 3f) % 1f
                drawCircle(
                    c.primary.copy(alpha = 0.4f * (1f - p)),
                    base * (1f + 0.65f * p),
                    mid,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
        drawCircle(
            Brush.radialGradient(
                listOf(c.primary, c.accent),
                center = Offset(mid.x - r * 0.25f, mid.y - r * 0.3f),
                radius = r * 1.5f,
            ),
            r,
            mid,
        )
        if (!still && state == OrbState.Thinking) {
            val ar = r * 1.22f
            drawArc(
                c.accent,
                spin,
                100f,
                false,
                Offset(mid.x - ar, mid.y - ar),
                Size(ar * 2, ar * 2),
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}
