package com.simha.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.simha.app.MainViewModel
import com.simha.app.Perm
import com.simha.app.Perms
import com.simha.app.R
import com.simha.app.data.CaregiverRow
import com.simha.app.data.Snap
import com.simha.app.llm.LlmConfig
import com.simha.app.llm.LlmException
import com.simha.app.llm.LlmPresets
import com.simha.app.llm.Msg
import com.simha.app.voice.SarvamVoiceAgent
import com.simha.app.voice.VoiceAgent
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
}

@Composable
fun SettingsScreen(vm: MainViewModel, s: Snap, onBack: () -> Unit, onAudit: () -> Unit) {
    val ctx = LocalContext.current
    val sl = vm.sl
    val scope = rememberCoroutineScope()
    val tick by vm.resume.collectAsState()
    val callLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { vm.resume.value += 1 }
    val title = stringResource(R.string.set_title)

    var inact by remember(s.inactivity) { mutableStateOf(s.inactivity.toString()) }
    var wait by remember(s.wait) { mutableStateOf(s.wait.toString()) }

    var cgs by remember { mutableStateOf<List<CaregiverRow>>(emptyList()) }
    var cgErr by remember { mutableStateOf<String?>(null) }
    val reload: () -> Unit = {
        scope.launch {
            sl.repo.caregivers().fold({ cgs = it.filter { c -> c.status != "removed" }; cgErr = null }, { cgErr = it.message ?: it.javaClass.simpleName })
        }
    }
    LaunchedEffect(Unit) { reload() }

    val stored = remember { sl.llmStore.load() }
    var base by remember { mutableStateOf(stored.getOrNull()?.baseUrl ?: "") }
    var key by remember { mutableStateOf(stored.getOrNull()?.apiKey ?: "") }
    var model by remember { mutableStateOf(stored.getOrNull()?.model ?: "") }
    var aiMsg by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }
    var agentMsg by remember { mutableStateOf<String?>(null) }
    val agent: VoiceAgent = remember { SarvamVoiceAgent() }

    ScreenFrame(title, title, onBack) {
        Section(stringResource(R.string.set_perms))
        Caption(stringResource(R.string.set_perm_hint))
        Perm.entries.filter { Perms.available(it) }.forEach { p ->
            val on = remember(tick, p) { Perms.granted(ctx, p) }
            SwitchRow(stringResource(permText(p).name), on, { Perms.open(ctx, p, false) })
        }
        if (!remember(tick) { Perms.granted(ctx, Perm.Usage) }) {
            Card { Body(stringResource(R.string.set_usage_denied, s.inactivity)) }
        }

        Section(stringResource(R.string.set_approvals))
        SwitchRow(stringResource(R.string.ap_family_label), s.callFamily, {
            vm.setApproval("call_family", it)
            if (it && !Perms.granted(ctx, Perm.CallPhone)) callLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
        })
        SwitchRow(stringResource(R.string.ap_hospital_label), s.callHospital, {
            vm.setApproval("call_hospital", it)
            if (it && !Perms.granted(ctx, Perm.CallPhone)) callLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
        })
        SwitchRow(stringResource(R.string.ap_record_label), s.record, { vm.setApproval("record", it) })

        Section(stringResource(R.string.set_rules))
        LabeledField(stringResource(R.string.set_inactivity), inact, { inact = it.filter(Char::isDigit) }, KeyboardType.Number, true)
        LabeledField(stringResource(R.string.set_wait), wait, { wait = it.filter(Char::isDigit) }, KeyboardType.Number, true)
        Caption(stringResource(R.string.set_rules_range))
        BigButton(stringResource(R.string.save), { vm.saveRules(inact.toIntOrNull() ?: s.inactivity, wait.toIntOrNull() ?: s.wait) })

        Section(stringResource(R.string.set_language))
        listOf("en" to R.string.lang_en, "hi" to R.string.lang_hi, "te" to R.string.lang_te, "kn" to R.string.lang_kn, "ta" to R.string.lang_ta)
            .forEach { (code, res) ->
                BigButton(stringResource(res), { vm.setLanguage(code) }, kind = if (s.lang == code) Kind.Primary else Kind.Secondary)
            }

        Section(stringResource(R.string.set_ai))
        stored.exceptionOrNull()?.let { Body(stringResource(R.string.ai_store_error, it.message ?: ""), color = MaterialTheme.colorScheme.error) }
        Body(stringResource(R.string.ai_preset))
        LlmPresets.all.forEach { (name, url) ->
            BigButton(name, {
                base = url
                aiMsg = null
            }, kind = if (base == url) Kind.Primary else Kind.Secondary)
        }
        BigButton(stringResource(R.string.preset_custom), { aiMsg = null }, kind = if (LlmPresets.all.none { it.second == base }) Kind.Primary else Kind.Secondary)
        LabeledField(stringResource(R.string.ai_base), base, { base = it }, KeyboardType.Uri, true)
        LabeledField(stringResource(R.string.ai_key), key, { key = it }, KeyboardType.Password, true, password = true)
        LabeledField(stringResource(R.string.ai_model), model, { model = it }, KeyboardType.Text, true)
        BigButton(stringResource(R.string.ai_save), {
            sl.llmStore.save(LlmConfig(base, key, model)).fold(
                { aiMsg = ctx.getString(R.string.ai_saved) },
                { aiMsg = ctx.getString(R.string.ai_store_error, it.message ?: "") },
            )
        })
        BigButton(stringResource(if (testing) R.string.ai_testing else R.string.ai_test), {
            val cfg = LlmConfig(base, key, model)
            if (!cfg.configured) {
                aiMsg = ctx.getString(R.string.ai_missing)
            } else {
                scope.launch {
                    testing = true
                    aiMsg = try {
                        val r = sl.llm.complete(cfg, JsonArray(listOf(Msg.user("Reply with the single word OK."))), null)
                        ctx.getString(R.string.ai_ok, (r.content ?: "").take(80))
                    } catch (e: LlmException) {
                        ctx.getString(R.string.ai_fail, e.message ?: "")
                    }
                    testing = false
                }
            }
        }, kind = Kind.Secondary, enabled = !testing)
        aiMsg?.let { Body(it) }

        Section(stringResource(R.string.set_agent))
        Caption(stringResource(R.string.agent_note))
        BigButton(stringResource(R.string.agent_try), {
            scope.launch {
                agent.call("checkin", "").fold(
                    { agentMsg = it },
                    { agentMsg = ctx.getString(R.string.agent_not_configured) },
                )
            }
        }, kind = Kind.Secondary)
        agentMsg?.let { Body(it) }

        Section(stringResource(R.string.set_caregivers))
        cgErr?.let { Body(stringResource(R.string.sync_error, it), color = MaterialTheme.colorScheme.error) }
        if (cgs.isEmpty()) Body(stringResource(R.string.cg_none))
        cgs.forEach { c ->
            Card {
                Body(stringResource(R.string.cg_code, c.inviteCode))
                Caption(
                    stringResource(
                        R.string.cg_status,
                        stringResource(if (c.status == "active") R.string.cg_active else R.string.cg_pending),
                    )
                )
                SwitchRow(stringResource(R.string.cg_view_rec), c.canViewRecordings, { on ->
                    scope.launch {
                        sl.repo.setCaregiverRecordings(c.id, on).onFailure { cgErr = it.message ?: it.javaClass.simpleName }
                        reload()
                    }
                })
                BigButton(stringResource(R.string.cg_remove), {
                    scope.launch {
                        sl.repo.removeCaregiver(c.id).onFailure { cgErr = it.message ?: it.javaClass.simpleName }
                        reload()
                    }
                }, kind = Kind.Secondary)
            }
        }
        BigButton(stringResource(R.string.cg_invite), {
            scope.launch {
                sl.repo.createInvite().fold({ reload() }, { cgErr = it.message ?: it.javaClass.simpleName })
            }
        }, kind = Kind.Secondary)

        BigButton(stringResource(R.string.set_audit), onAudit, kind = Kind.Secondary)
        if (s.running) {
            BigButton(stringResource(R.string.set_stop), { vm.stopSimha() })
        } else {
            Body(stringResource(R.string.stopped_banner))
            BigButton(stringResource(R.string.set_start), { vm.startSimha() })
        }
    }
}
