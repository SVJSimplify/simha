package com.simha.app.ui.talk

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simha.app.Actions
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.ui.BigButton
import com.simha.app.ui.Body
import com.simha.app.ui.Caption
import com.simha.app.ui.Card
import com.simha.app.ui.Kind
import com.simha.app.ui.LabeledField
import com.simha.app.ui.ScreenFrame
import com.simha.app.voice.VoiceIssue

@Composable
fun TalkScreen(
    checkin: Boolean,
    onBack: () -> Unit,
    onEmergency: () -> Unit,
    onSettings: () -> Unit,
    vm: TalkViewModel = viewModel(),
) {
    val ctx = LocalContext.current
    val sl = (ctx.applicationContext as SimhaApp).sl
    val view = LocalView.current
    val issue by sl.voice.issue.collectAsState()
    val sarvamErr by sl.voice.sarvamError.collectAsState()
    val title = stringResource(if (checkin) R.string.ci_q else R.string.talk_title)
    var typed by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val initLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.begin(checkin) }
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
        if (Actions.has(ctx, Manifest.permission.RECORD_AUDIO)) vm.begin(checkin) else initLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }
    LaunchedEffect(Unit) { vm.events.collect { onEmergency() } }
    LaunchedEffect(Unit) { vm.cameraRequest.collect { if (it != null) photoLauncher.launch(null) } }

    ScreenFrame(title, title, { vm.stop(); onBack() }) {
        if (!vm.configured) {
            Card {
                Body(stringResource(R.string.talk_not_set))
                BigButton(stringResource(R.string.talk_open_settings), onSettings, kind = Kind.Secondary)
            }
        }
        if (issue == VoiceIssue.Unavailable) Body(stringResource(R.string.voice_unavailable))
        sarvamErr?.let { Body(stringResource(R.string.sarvam_fallback, it), color = MaterialTheme.colorScheme.error) }
        if (issue == VoiceIssue.LangMissing) Body(stringResource(R.string.voice_lang_missing))
        vm.error?.let { e ->
            Body(
                if (e.arg != null) stringResource(e.res, e.arg) else stringResource(e.res),
                color = MaterialTheme.colorScheme.error,
            )
        }
        val canTalk = vm.configured || checkin
        if (canTalk) {
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
            if (vm.partial.isNotEmpty()) Body(vm.partial)
            if (vm.live.isNotEmpty()) Card { Body(vm.live) }
            if (vm.recording) {
                Card {
                    Body(stringResource(R.string.talk_recording))
                    BigButton(stringResource(R.string.em_record_stop), { vm.stopRecording() }, kind = Kind.Success)
                }
            }
            BigButton(
                stringResource(if (vm.mode == TalkMode.Listening) R.string.talk_listening else R.string.talk_mic),
                {
                    if (Actions.has(ctx, Manifest.permission.RECORD_AUDIO)) vm.tapMic() else micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
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
            BigButton(stringResource(R.string.talk_stop), { vm.stop() }, kind = Kind.Secondary, icon = R.drawable.ic_stop)
            LabeledField(stringResource(R.string.talk_type), typed, { typed = it })
            BigButton(stringResource(R.string.talk_send), {
                vm.submitText(typed)
                typed = ""
            }, kind = Kind.Secondary, enabled = typed.isNotBlank())
        }
        if (checkin || !vm.configured) {
            if (vm.checkedIn) {
                Body(stringResource(R.string.ci_thanks), color = MaterialTheme.colorScheme.tertiary)
            } else {
                BigButton(stringResource(R.string.ci_fine), { vm.fine() }, kind = Kind.Success)
                BigButton(stringResource(R.string.ci_help), { vm.help() }, kind = Kind.Emergency)
            }
        }
        if (!checkin) {
            BigButton(stringResource(R.string.help), { vm.stop(); onEmergency() }, kind = Kind.Emergency)
        }
        vm.lines.asReversed().forEach { l ->
            Card {
                Caption(stringResource(if (l.fromUser) R.string.talk_you else R.string.talk_simha))
                Body(l.text)
            }
        }
    }
}
