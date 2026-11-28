package com.simha.app.ui

import android.provider.Settings
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.logic.Ago
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.ui.theme.SimhaRadius
import kotlinx.coroutines.launch

data class Layout(val columns: Int, val pad: Dp, val target: Dp, val primaryH: Dp)

val LocalLayout = staticCompositionLocalOf { Layout(1, 16.dp, 56.dp, 64.dp) }
val LocalLang = compositionLocalOf { "en" }

@Composable
fun rememberLayout(wsc: WindowWidthSizeClass): Layout {
    val big = LocalDensity.current.fontScale > 1.3f
    val cols = when (wsc) {
        WindowWidthSizeClass.Compact -> 1
        WindowWidthSizeClass.Medium -> 2
        else -> 3
    }
    val pad = when (wsc) {
        WindowWidthSizeClass.Compact -> 16.dp
        WindowWidthSizeClass.Medium -> 24.dp
        else -> 32.dp
    }
    return Layout(cols, pad, if (big) 64.dp else 56.dp, if (big) 72.dp else 64.dp)
}

@Composable
fun animMs(): Int {
    val ctx = LocalContext.current
    return remember(ctx) {
        if (Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) 0 else 200
    }
}

val EmphasizedEasing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

const val NavMs = 250

fun Modifier.pressScale(src: InteractionSource, enabled: Boolean = true): Modifier = composed {
    val pressed by src.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current
    val spec: AnimationSpec<Float> = if (animMs() == 0) snap() else spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
    val sc by animateFloatAsState(if (pressed && enabled) 0.96f else 1f, spec, label = "press")
    LaunchedEffect(pressed) { if (pressed && enabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
    graphicsLayer {
        scaleX = sc
        scaleY = sc
    }
}

fun Modifier.pressClick(
    onClick: () -> Unit,
    enabled: Boolean = true,
    role: Role? = Role.Button,
): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    this
        .pressScale(src, enabled)
        .clickable(interactionSource = src, indication = null, enabled = enabled, role = role, onClick = onClick)
}

@Composable
fun AnimatedEntrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val still = animMs() == 0
    var shown by remember { mutableStateOf(still) }
    LaunchedEffect(Unit) { shown = true }
    val spec: AnimationSpec<Float> = if (still) snap() else tween(350, delayMillis = index * 40, easing = EmphasizedEasing)
    val a by animateFloatAsState(if (shown) 1f else 0f, spec, label = "entrance")
    val dy = with(LocalDensity.current) { 16.dp.toPx() }
    Box(
        modifier.graphicsLayer {
            alpha = a
            translationY = (1f - a) * dy
        },
    ) { content() }
}

enum class Kind { Primary, Emergency, Secondary, Success }

@Composable
fun ScreenFrame(
    title: String,
    read: String,
    onBack: (() -> Unit)?,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val l = LocalLayout.current
    val lang = LocalLang.current
    val voice = (LocalContext.current.applicationContext as SimhaApp).sl.voice
    val scope = rememberCoroutineScope()
    val c = LocalSimha.current
    CompositionLocalProvider(LocalContentColor provides c.text) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.page)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(l.pad),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (onBack != null) {
                            Box(
                                Modifier.size(maxOf(l.target, 64.dp)).pressClick(onBack, role = Role.Button),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back), Modifier.size(32.dp))
                            }
                        }
                        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(l.target).pressClick({ scope.launch { voice.say(read, lang) } }, role = Role.Button),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(painterResource(R.drawable.ic_speaker), stringResource(R.string.read_aloud))
                        }
                    }
                    AnimatedEntrance(0, Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
                    }
                }
                if (bottom != null) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = l.pad, vertical = 12.dp)) { bottom() }
                }
            }
        }
    }
}

@Composable
private fun ButtonCore(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: Int?,
    enabled: Boolean,
    bg: Color,
    fg: Color,
    stroke: Color?,
) {
    val l = LocalLayout.current
    val shape = RoundedCornerShape(SimhaRadius)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = l.primaryH)
            .pressClick(onClick, enabled)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(bg)
            .then(if (stroke != null) Modifier.border(1.dp, stroke, shape) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(28.dp), tint = fg)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = fg, textAlign = TextAlign.Center)
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null, enabled: Boolean = true) {
    val c = LocalSimha.current
    ButtonCore(text, onClick, modifier, icon, enabled, c.primary, c.onPrimary, null)
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: Int? = null, enabled: Boolean = true) {
    val c = LocalSimha.current
    ButtonCore(text, onClick, modifier, icon, enabled, c.surface, c.text, c.border)
}

@Composable
fun EmergencyButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalSimha.current
    ButtonCore(text, onClick, modifier, null, true, c.emergency, Color.White, null)
}

@Composable
fun TextLink(text: String, onClick: () -> Unit) {
    val l = LocalLayout.current
    Box(
        Modifier
            .heightIn(min = l.target)
            .pressClick(onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp, textDecoration = TextDecoration.Underline),
            color = LocalSimha.current.text2,
        )
    }
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: Kind = Kind.Primary,
    enabled: Boolean = true,
    icon: Int? = null,
) {
    val c = LocalSimha.current
    when (kind) {
        Kind.Primary -> PrimaryButton(text, onClick, modifier, icon, enabled)
        Kind.Secondary -> SecondaryButton(text, onClick, modifier, icon, enabled)
        Kind.Emergency -> ButtonCore(text, onClick, modifier, icon, enabled, c.emergency, Color.White, null)
        Kind.Success -> ButtonCore(text, onClick, modifier, icon, enabled, c.success, Color.White, null)
    }
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = modifier, color = color)
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = LocalSimha.current.text2, modifier = modifier)
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = LocalSimha.current.text,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = LocalSimha.current
    Surface(
        color = c.surface,
        contentColor = c.text,
        shape = RoundedCornerShape(SimhaRadius),
        border = BorderStroke(1.dp, c.border),
        modifier = modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    error: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    password: Boolean = false,
    singleLine: Boolean = true,
) {
    val c = LocalSimha.current
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            if (required) "$label *" else label,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = error != null,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            shape = RoundedCornerShape(SimhaRadius),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.text,
                unfocusedTextColor = c.text,
                errorTextColor = c.text,
                focusedContainerColor = c.surface,
                unfocusedContainerColor = c.surface,
                errorContainerColor = c.surface,
                focusedBorderColor = c.primary,
                unfocusedBorderColor = c.border,
                errorBorderColor = c.emergency,
                cursorColor = c.primary,
                errorCursorColor = c.emergency,
            ),
        )
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = c.emergency)
        }
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    val l = LocalLayout.current
    val c = LocalSimha.current
    val src = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = l.target)
            .pressScale(src)
            .toggleable(value = checked, interactionSource = src, indication = null, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Body(label)
            if (hint != null) Caption(hint)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = c.onPrimary,
                checkedTrackColor = c.primary,
                uncheckedThumbColor = c.text2,
                uncheckedTrackColor = c.fill,
                uncheckedBorderColor = c.border,
            ),
        )
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val l = LocalLayout.current
    val c = LocalSimha.current
    val shape = RoundedCornerShape(SimhaRadius)
    Box(
        modifier
            .heightIn(min = l.target)
            .pressClick(onClick, role = Role.RadioButton)
            .semantics { this.selected = selected }
            .clip(shape)
            .background(if (selected) c.fill else c.surface)
            .border(1.dp, if (selected) c.primary else c.border, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
            color = c.text,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ChipGroup(items: List<String>, selected: Int, onSelect: (Int) -> Unit, perRow: Int = 3) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.indices.chunked(perRow).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { i -> Chip(items[i], i == selected, { onSelect(i) }, Modifier.weight(1f)) }
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun OptionRow(text: String, selected: Boolean, onClick: () -> Unit, hint: String? = null) {
    val l = LocalLayout.current
    val c = LocalSimha.current
    val shape = RoundedCornerShape(SimhaRadius)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = l.primaryH)
            .pressClick(onClick, role = Role.RadioButton)
            .semantics { this.selected = selected }
            .clip(shape)
            .background(if (selected) c.fill else c.surface)
            .border(1.dp, if (selected) c.primary else c.border, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Body(text)
            if (hint != null) Caption(hint)
        }
        Box(
            Modifier.size(24.dp).border(2.dp, if (selected) c.primary else c.border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) Box(Modifier.size(12.dp).background(c.primary, CircleShape))
        }
    }
}

@Composable
fun Grid(cols: Int, items: List<@Composable (Modifier) -> Unit>) {
    items.chunked(cols).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { it(Modifier.weight(1f)) }
            repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}

@Composable
fun TabFrame(
    top: (@Composable () -> Unit)? = null,
    bottom: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val l = LocalLayout.current
    val c = LocalSimha.current
    CompositionLocalProvider(LocalContentColor provides c.text) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.page)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                top?.invoke()
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = l.pad, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = content,
                )
                if (bottom != null) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = l.pad, vertical = 12.dp)) { bottom() }
                }
            }
        }
    }
}

@Composable
fun NavRow(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, hint: String? = null, icon: Int? = null, tint: Color = Color.Unspecified) {
    val l = LocalLayout.current
    val c = LocalSimha.current
    val shape = RoundedCornerShape(SimhaRadius)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = l.primaryH)
            .pressClick(onClick)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (icon != null) Icon(painterResource(icon), null, Modifier.size(28.dp), tint = if (tint == Color.Unspecified) c.primary else tint)
        Column(Modifier.weight(1f)) {
            Body(text)
            if (hint != null) Caption(hint)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = c.text2)
    }
}

@Composable
fun agoText(a: Ago): String = when (a) {
    Ago.Never -> stringResource(R.string.ago_never)
    Ago.Now -> stringResource(R.string.ago_now)
    is Ago.Minutes -> stringResource(R.string.ago_min, a.n)
    is Ago.Hours -> stringResource(R.string.ago_hour, a.n)
    is Ago.Days -> stringResource(R.string.ago_day, a.n)
}

@Composable
fun roleLabel(role: String): String = stringResource(
    when (role) {
        "family" -> R.string.role_family
        "neighbor" -> R.string.role_neighbor
        "doctor" -> R.string.role_doctor
        else -> R.string.role_hospital
    }
)

fun normalizeContactPhone(raw: String): String? {
    if (raw.any { !(it.isDigit() || it in "+ -()") }) return null
    val d = raw.filter { it.isDigit() }
    val n = when {
        d.length == 12 && d.startsWith("91") -> d.substring(2)
        d.length == 11 && d.startsWith("0") -> d.substring(1)
        else -> d
    }
    return if (n.length == 10) n else null
}

@Composable
fun ProvideLayout(wsc: WindowWidthSizeClass, lang: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayout provides rememberLayout(wsc), LocalLang provides lang, content = content)
}
