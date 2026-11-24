package com.simha.app.ui.talk

import android.Manifest
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simha.app.Actions
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.ui.BigButton
import com.simha.app.ui.Body
import com.simha.app.ui.Caption
import com.simha.app.ui.Card
import com.simha.app.ui.Kind
import com.simha.app.ui.animMs
import com.simha.app.ui.LabeledField
import com.simha.app.ui.home.OrbState
import com.simha.app.ui.home.SimhaOrb
import com.simha.app.voice.VoiceIssue
import com.simha.app.voice.WakeWord
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.wake.WakeReason
import kotlinx.coroutines.launch

@Composable
private fun FadeWords(text: String, color: Color, modifier: Modifier = Modifier) {
    val words = remember(text) { text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() } }
    val still = animMs() == 0
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(words.size, still) {
        val n = words.size.toFloat()
        if (still || n < reveal.value) reveal.snapTo(n) else reveal.animateTo(n, tween(((n - reveal.value) * 110).toInt().coerceAtLeast(1), easing = LinearEasing))
    }
    val r = reveal.value
    val a: AnnotatedString = buildAnnotatedString {
        words.forEachIndexed { i, w ->
            withStyle(SpanStyle(color = color.copy(alpha = (r - i).coerceIn(0f, 1f)))) { append(w) }
            if (i < words.lastIndex) append(" ")
        }
    }
    Text(a, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = modifier.fillMaxWidth())
}

@Composable
fun ConversationPanel(
    reason: WakeReason?,
    extra: String?,
    onDone: () -> Unit,
    onEmergency: (() -> Unit)? = null,
    unwell: Boolean = false,
    vm: TalkViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val sl = (ctx.applicationContext as SimhaApp).sl
    val scope = rememberCoroutineScope()
    val issue by sl.voice.issue.collectAsState()
    val sarvamErr by sl.voice.sarvamError.collectAsState()
    val level by sl.voice.level.collectAsState()
    var typed by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        WakeWord.stopWakeWord()
        onDispose {
            if (generateSequence(ctx) { (it as? android.content.ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()?.isChangingConfigurations != true) vm.reset()
        }
    }
    val initLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.begin(reason, extra, unwell) }
    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.tapMic() else vm.error = UiMsg(R.string.talk_mic_needed)
    }
    val camPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) vm.camera() else vm.error = UiMsg(R.string.talk_cam_needed)
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        vm.cameraRequest.value?.complete(bmp)
        vm.cameraRequest.value = null
    }
    LaunchedEffect(Unit) {
        vm.events.collect { e ->
            when (e) {
                TalkEvent.OpenEmergency -> if (onEmergency != null) onEmergency() else scope.launch { sl.repo.dialEmergency("conversation") }
                TalkEvent.Done -> onDone()
            }
        }
    }
    LaunchedEffect(Unit) { vm.cameraRequest.collect { if (it != null) photoLauncher.launch(null) } }
    LaunchedEffect(Unit) {
        if (Actions.has(ctx, Manifest.permission.RECORD_AUDIO)) vm.begin(reason, extra, unwell) else initLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val canTalk = vm.configured || reason == WakeReason.Checkin
    val orb = when (vm.mode) {
        TalkMode.Idle -> OrbState.Idle
        TalkMode.Listening -> OrbState.Listening
        TalkMode.Thinking -> OrbState.Thinking
        TalkMode.Speaking -> OrbState.Speaking
    }
    fun mic() {
        if (Actions.has(ctx, Manifest.permission.RECORD_AUDIO)) vm.tapMic() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    Column(
        Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (issue == VoiceIssue.Unavailable) Body(stringResource(R.string.voice_unavailable))
        if (issue == VoiceIssue.LangMissing) Body(stringResource(R.string.voice_lang_missing))
        sarvamErr?.let { Body(stringResource(R.string.sarvam_fallback, it), color = MaterialTheme.colorScheme.error) }
        vm.error?.let { e ->
            Body(
                if (e.arg != null) stringResource(e.res, e.arg!!) else stringResource(e.res),
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (!canTalk && reason != null) Body(stringResource(R.string.talk_not_set))
        SimhaOrb(orb, { if (canTalk && !vm.recording) mic() }, Modifier.size(220.dp), level)
        Caption(
            stringResource(
                when (vm.mode) {
                    TalkMode.Listening -> R.string.talk_listening
                    TalkMode.Thinking -> R.string.talk_thinking
                    TalkMode.Speaking -> R.string.talk_speaking
                    TalkMode.Idle -> R.string.talk_idle
                }
            )
        )
        val sc = LocalSimha.current
        if (vm.partial.isNotEmpty()) key("u") { FadeWords(vm.partial, sc.text2) }
        else if (vm.live.isNotEmpty()) key("s") { FadeWords(vm.live, sc.text) }
        if (vm.recording) {
            Card {
                Body(stringResource(R.string.talk_recording))
                BigButton(stringResource(R.string.em_record_stop), { vm.stopRecording() }, kind = Kind.Success)
            }
        }
        if (canTalk) {
            BigButton(
                stringResource(if (vm.mode == TalkMode.Listening) R.string.talk_listening else R.string.talk_mic),
                { mic() },
                icon = R.drawable.ic_mic,
                enabled = !vm.recording,
            )
            if (vm.configured) {
                BigButton(
                    stringResource(R.string.talk_camera),
                    {
                        if (Actions.has(ctx, Manifest.permission.CAMERA)) vm.camera() else camPermLauncher.launch(Manifest.permission.CAMERA)
                    },
                    kind = Kind.Secondary,
                    icon = R.drawable.ic_camera,
                )
            }
            BigButton(stringResource(R.string.talk_stop), { vm.stop(); onDone() }, kind = Kind.Secondary, icon = R.drawable.ic_stop)
            LabeledField(stringResource(R.string.talk_type), typed, { typed = it })
            BigButton(
                stringResource(R.string.talk_send),
                {
                    vm.submitText(typed)
                    typed = ""
                },
                kind = Kind.Secondary,
                enabled = typed.isNotBlank(),
            )
        }
        val shown = if (vm.live.isNotEmpty() && vm.lines.lastOrNull()?.fromUser == false) vm.lines.dropLast(1) else vm.lines.toList()
        shown.takeLast(6).asReversed().forEach { l ->
            Card {
                Caption(stringResource(if (l.fromUser) R.string.talk_you else R.string.talk_simha))
                Body(l.text)
            }
        }
    }
}
