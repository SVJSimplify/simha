package com.simha.app.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
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
    val ms = animMs()
    val still = ms == 0
    val t = rememberInfiniteTransition(label = "orb")
    val breathe by t.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(3200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe",
    )
    val ripple by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "ripple")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "spin")
    val b0 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b0")
    val b1 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(560, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b1")
    val b2 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(480, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b2")
    val b3 by t.animateFloat(0f, 1f, infiniteRepeatable(tween(640, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "b3")
    val ringA by animateFloatAsState(if (state == OrbState.Listening) 1f else 0f, tween(ms), label = "ringA")
    val arcA by animateFloatAsState(if (state == OrbState.Thinking) 1f else 0f, tween(ms), label = "arcA")
    val desc = stringResource(R.string.r3_orb_caption_tap)
    val hi = lerp(c.primary, Color.White, 0.12f)
    val s = if (still) 1f else 1f + 0.03f * breathe

    Box(
        modifier
            .size(168.dp)
            .aspectRatio(1f)
            .pressClick(onTap)
            .semantics(mergeDescendants = true) { contentDescription = desc },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val mid = Offset(size.width / 2f, size.height / 2f)
            val r = size.minDimension * 0.37f
            if (!still && ringA > 0f) {
                for (i in 0 until 2) {
                    val p = (ripple + i * 0.5f) % 1f
                    drawCircle(
                        c.primary.copy(alpha = 0.3f * (1f - p) * ringA),
                        r + (size.minDimension / 2f - r - 1.dp.toPx()) * p,
                        mid,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
            if (!still && arcA > 0f) {
                val ar = r + 8.dp.toPx()
                drawArc(
                    c.accent.copy(alpha = arcA),
                    spin,
                    90f,
                    false,
                    Offset(mid.x - ar, mid.y - ar),
                    Size(ar * 2, ar * 2),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        Box(
            Modifier
                .size(124.dp)
                .graphicsLayer {
                    scaleX = s
                    scaleY = s
                    shadowElevation = 8.dp.toPx()
                    shape = CircleShape
                    clip = false
                    ambientShadowColor = c.primary
                    spotShadowColor = c.primary
                }
                .drawBehind {
                    drawCircle(c.primary)
                    drawCircle(
                        Brush.radialGradient(
                            listOf(hi, c.primary),
                            center = Offset(size.width * 0.3f, size.height * 0.28f),
                            radius = size.width * 0.8f,
                        )
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Crossfade(state == OrbState.Speaking, animationSpec = tween(ms), label = "glyph") { speaking ->
                if (speaking) {
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        listOf(b0, b1, b2, b3).forEachIndexed { i, v ->
                            val f = if (still) 0.5f else v
                            Spacer(
                                Modifier
                                    .width(5.dp)
                                    .height((10f + 24f * f).dp)
                                    .background(c.onPrimary, RoundedCornerShape(50))
                            )
                        }
                    }
                } else {
                    Image(
                        painterResource(R.drawable.ic_mic),
                        null,
                        Modifier.size(44.dp),
                        colorFilter = ColorFilter.tint(c.onPrimary),
                    )
                }
            }
        }
    }
}
