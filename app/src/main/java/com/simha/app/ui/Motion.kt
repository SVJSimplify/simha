package com.simha.app.ui

import android.os.Build
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.ui.theme.SimhaRadius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun StepBar(step: Int, total: Int, modifier: Modifier = Modifier) {
    val c = LocalSimha.current
    val still = animMs() == 0
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(total) { i ->
            val f by animateFloatAsState(
                if (i <= step) 1f else 0f,
                if (still) snap() else tween(500, delayMillis = if (i == step) 120 else 0, easing = EmphasizedEasing),
                label = "step$i",
            )
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(c.fill)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(CircleShape).background(c.primary))
            }
        }
    }
}

@Composable
fun BlurInText(text: String, style: TextStyle, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    val still = animMs() == 0
    var shown by remember { mutableStateOf(still) }
    LaunchedEffect(Unit) { shown = true }
    val a by animateFloatAsState(if (shown) 1f else 0f, if (still) snap() else tween(900, easing = EmphasizedEasing), label = "blurIn")
    val dy = with(LocalDensity.current) { 8.dp.toPx() }
    val m = if (Build.VERSION.SDK_INT >= 31 && a < 1f) Modifier.blur(((1f - a) * 10f).dp) else Modifier
    Text(
        text,
        style = style,
        color = color,
        modifier = modifier.then(m).graphicsLayer {
            alpha = a
            translationY = (1f - a) * dy
        },
    )
}

@Composable
fun SlideToConfirm(text: String, icon: Int, onConfirm: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalSimha.current
    val l = LocalLayout.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val still = animMs() == 0
    val h = l.primaryH.coerceAtLeast(64.dp)
    val pad = 4.dp
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(h)
            .clip(RoundedCornerShape(SimhaRadius))
            .background(c.fill)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = text
                onClick { onConfirm(); true }
            },
    ) {
        val d = LocalDensity.current
        val thumb = h - pad * 2
        val maxPx = with(d) { (maxWidth - thumb - pad * 2).toPx() }.coerceAtLeast(1f)
        val x = remember { Animatable(0f) }
        var fired by remember { mutableStateOf(false) }
        val frac = x.value / maxPx
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = c.text,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = thumb + pad * 2, end = 12.dp)
                .graphicsLayer { alpha = 1f - frac * 0.8f },
        )
        Box(
            Modifier
                .padding(pad)
                .offset { IntOffset(x.value.roundToInt(), 0) }
                .size(thumb)
                .clip(RoundedCornerShape(SimhaRadius - pad))
                .background(c.primary)
                .draggable(
                    rememberDraggableState { dx -> scope.launch { x.snapTo((x.value + dx).coerceIn(0f, maxPx)) } },
                    Orientation.Horizontal,
                    onDragStopped = {
                        if (x.value >= maxPx * 0.85f && !fired) {
                            fired = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            x.animateTo(maxPx, if (still) snap() else tween(150))
                            onConfirm()
                            delay(700)
                            fired = false
                        }
                        x.animateTo(0f, if (still) snap() else spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow))
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, Modifier.size(28.dp), tint = c.onPrimary)
        }
    }
}

@Composable
fun TickMark(drawn: Boolean, color: Color, modifier: Modifier = Modifier, size: Dp = 28.dp) {
    val still = animMs() == 0
    val p by animateFloatAsState(if (drawn) 1f else 0f, if (still) snap() else tween(520, easing = EmphasizedEasing), label = "tick")
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.18f, w * 0.52f)
            lineTo(w * 0.42f, w * 0.75f)
            lineTo(w * 0.84f, w * 0.28f)
        }
        val pm = PathMeasure().apply { setPath(path, false) }
        val seg = Path()
        pm.getSegment(0f, pm.length * p, seg, true)
        drawPath(seg, color, style = Stroke(width = w * 0.11f, cap = StrokeCap.Round))
    }
}

@Composable
fun Waveform(level: Float, active: Boolean, modifier: Modifier = Modifier) {
    val c = LocalSimha.current
    val still = animMs() == 0
    val t by rememberInfiniteTransition(label = "wave").animateFloat(
        0f,
        (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "waveT",
    )
    val lv by animateFloatAsState(if (active) level.coerceIn(0f, 1f) else 0f, if (still) snap() else spring(stiffness = Spring.StiffnessMediumLow), label = "waveL")
    val show by animateFloatAsState(if (active) 1f else 0f, tween(if (still) 0 else 300), label = "waveA")
    Row(
        modifier.height(40.dp).graphicsLayer { alpha = show },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { i ->
            val wob = if (still) 0.5f else 0.5f + 0.5f * sin(t + i * 0.9f)
            val hFrac = 0.2f + 0.8f * (0.35f * wob + 0.65f * lv * (0.6f + 0.4f * wob))
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight(hFrac.coerceIn(0.2f, 1f))
                    .clip(CircleShape)
                    .background(if (i == 2) c.accent else c.primary),
            )
        }
    }
}

@Composable
fun ThinkingText(text: String, modifier: Modifier = Modifier) {
    val c = LocalSimha.current
    val still = animMs() == 0
    val words = remember(text) { text.trim().split(Regex("\\s+")) }
    val t by rememberInfiniteTransition(label = "think").animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "thinkT",
    )
    val spin by rememberInfiniteTransition(label = "spin").animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "spinR",
    )
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.size(22.dp).graphicsLayer { rotationZ = if (still) 0f else spin }) {
            val sw = size.width * 0.14f
            drawArc(c.fill, 0f, 360f, false, style = Stroke(sw))
            drawArc(c.primary, -90f, 100f, false, style = Stroke(sw, cap = StrokeCap.Round))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            words.forEachIndexed { i, w ->
                val ph = ((t - i * 0.15f) % 1f + 1f) % 1f
                val a = if (still) 1f else 0.4f + 0.6f * (0.5f + 0.5f * sin(ph * 2f * Math.PI.toFloat()))
                Text(w, style = MaterialTheme.typography.bodySmall, color = c.text2, modifier = Modifier.graphicsLayer { alpha = a })
            }
        }
    }
}

@Composable
fun TakenRow(label: String, onTaken: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalSimha.current
    val l = LocalLayout.current
    var done by remember { mutableStateOf(false) }
    LaunchedEffect(done) {
        if (done) {
            delay(650)
            onTaken()
            delay(1200)
            done = false
        }
    }
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = l.primaryH)
            .pressClick({ if (!done) done = true })
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(24.dp).clip(CircleShape).background(if (done) c.success else c.fill), contentAlignment = Alignment.Center) {
            TickMark(done, Color.White, size = 20.dp)
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
