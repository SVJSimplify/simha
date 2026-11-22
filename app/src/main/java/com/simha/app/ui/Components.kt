package com.simha.app.ui

import android.provider.Settings
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.logic.Ago
import com.simha.app.ui.theme.Coral
import com.simha.app.ui.theme.EmergencyRed
import kotlinx.coroutines.launch

data class Layout(val columns: Int, val pad: Dp, val target: Dp, val primaryH: Dp)

val LocalLayout = staticCompositionLocalOf { Layout(1, 16.dp, 56.dp, 72.dp) }
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
    return Layout(cols, pad, if (big) 64.dp else 56.dp, if (big) 88.dp else 72.dp)
}

@Composable
fun animMs(): Int {
    val ctx = LocalContext.current
    return if (Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) 0 else 200
}

enum class Kind { Primary, Emergency, Secondary, Success }

@Composable
fun ScreenFrame(
    title: String,
    read: String,
    onBack: (() -> Unit)?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val l = LocalLayout.current
    val lang = LocalLang.current
    val voice = (LocalContext.current.applicationContext as SimhaApp).sl.voice
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(l.pad),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack, modifier = Modifier.size(l.target)) {
                        Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
                    }
                }
                Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { scope.launch { voice.say(read, lang) } },
                    modifier = Modifier.size(l.target),
                ) {
                    Icon(painterResource(R.drawable.ic_speaker), stringResource(R.string.read_aloud))
                }
            }
            HorizontalDivider(color = Coral, thickness = 2.dp)
            content()
        }
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
    val l = LocalLayout.current
    val base = modifier.fillMaxWidth()
    val body: @Composable () -> Unit = {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, textAlign = TextAlign.Center)
    }
    when (kind) {
        Kind.Secondary -> OutlinedButton(onClick, base.heightIn(min = l.target), enabled = enabled) { body() }
        Kind.Emergency -> Button(
            onClick,
            base.heightIn(min = l.primaryH),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed, contentColor = Color.White),
        ) { body() }
        Kind.Success -> Button(
            onClick,
            base.heightIn(min = l.primaryH),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
        ) { body() }
        Kind.Primary -> Button(onClick, base.heightIn(min = l.primaryH), enabled = enabled) { body() }
    }
}

@Composable
fun Body(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    Text(text, style = MaterialTheme.typography.bodyLarge, modifier = modifier, color = color)
}

@Composable
fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@Composable
fun Card(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = false,
    password: Boolean = false,
) {
    val l = LocalLayout.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Body(label)
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = l.target),
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            singleLine = singleLine,
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        )
    }
}

@Composable
fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit, hint: String? = null) {
    val l = LocalLayout.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = l.target)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Body(label)
            if (hint != null) Caption(hint)
        }
        Switch(checked = checked, onCheckedChange = null)
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

@Composable
fun ProvideLayout(wsc: WindowWidthSizeClass, lang: String, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayout provides rememberLayout(wsc), LocalLang provides lang, content = content)
}
