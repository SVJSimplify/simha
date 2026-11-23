package com.simha.app.ui.talk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.llm.LlmConfig
import com.simha.app.llm.LlmException
import com.simha.app.llm.Msg
import com.simha.app.llm.SarvamConfig
import com.simha.app.ui.Body
import com.simha.app.ui.Caption
import com.simha.app.ui.LabeledField
import com.simha.app.ui.PrimaryButton
import com.simha.app.ui.SecondaryButton
import com.simha.app.ui.SectionTitle
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray

@Composable
fun AiSettingsSection() {
    val ctx = LocalContext.current
    val sl = (ctx.applicationContext as SimhaApp).sl
    val scope = rememberCoroutineScope()
    val loaded = remember { sl.llmStore.load() }
    val l0 = remember { loaded.getOrNull() ?: LlmConfig() }
    val s0 = remember { sl.llmStore.sarvam() }
    val sarvamClient = remember { com.simha.app.voice.SarvamClient() }
    var base by remember { mutableStateOf(l0.baseUrl) }
    var key by remember { mutableStateOf(l0.apiKey) }
    var model by remember { mutableStateOf(l0.model) }
    var sKey by remember { mutableStateOf(s0.apiKey) }
    var agentKey by remember { mutableStateOf(s0.agentKey) }
    var org by remember { mutableStateOf(s0.orgId) }
    var ws by remember { mutableStateOf(s0.workspaceId) }
    var appId by remember { mutableStateOf(s0.appId) }
    var ver by remember { mutableStateOf(s0.appVersion) }
    var conn by remember { mutableStateOf(s0.connectionId) }
    var phone by remember { mutableStateOf(s0.agentPhone) }
    var llmMsg by remember { mutableStateOf<String?>(null) }
    var sMsg by remember { mutableStateOf<String?>(null) }
    var saveMsg by remember { mutableStateOf<String?>(null) }
    var testingLlm by remember { mutableStateOf(false) }
    var testingS by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(R.string.aiset_llm))
        loaded.exceptionOrNull()?.let { Body(stringResource(R.string.aiset_store_error, it.message ?: ""), color = MaterialTheme.colorScheme.error) }
        LabeledField(stringResource(R.string.aiset_base), base, { base = it }, Modifier, true, null, KeyboardType.Uri)
        LabeledField(stringResource(R.string.aiset_key), key, { key = it }, Modifier, true, null, KeyboardType.Password, true)
        LabeledField(stringResource(R.string.aiset_model), model, { model = it }, Modifier, true)
        SecondaryButton(
            stringResource(if (testingLlm) R.string.aiset_testing else R.string.aiset_test),
            {
                val cfg = LlmConfig(base, key, model)
                if (!cfg.configured) {
                    llmMsg = ctx.getString(R.string.aiset_missing)
                } else {
                    scope.launch {
                        testingLlm = true
                        llmMsg = try {
                            val r = sl.llm.complete(cfg, JsonArray(listOf(Msg.user("Reply with the single word OK."))), null)
                            ctx.getString(R.string.aiset_ok, (r.content ?: "").take(80))
                        } catch (e: LlmException) {
                            ctx.getString(R.string.aiset_fail, e.message ?: "")
                        }
                        testingLlm = false
                    }
                }
            },
            enabled = !testingLlm,
        )
        llmMsg?.let { Body(it) }

        SectionTitle(stringResource(R.string.aiset_sarvam))
        Caption(stringResource(R.string.aiset_sarvam_hint))
        LabeledField(stringResource(R.string.aiset_sarvam_key), sKey, { sKey = it }, Modifier, false, null, KeyboardType.Password, true)
        SecondaryButton(
            stringResource(if (testingS) R.string.aiset_testing else R.string.aiset_test_sarvam),
            {
                if (sKey.isBlank()) {
                    sMsg = ctx.getString(R.string.aiset_sarvam_missing)
                } else {
                    scope.launch {
                        testingS = true
                        sMsg = sarvamClient.tts(sKey.trim(), "OK", "en-IN").fold(
                            { ctx.getString(R.string.aiset_sarvam_ok, it.size / 1024) },
                            { ctx.getString(R.string.aiset_fail, it.message ?: "") },
                        )
                        testingS = false
                    }
                }
            },
            enabled = !testingS,
        )
        sMsg?.let { Body(it) }

        SectionTitle(stringResource(R.string.aiset_agent))
        Caption(stringResource(R.string.aiset_agent_hint))
        LabeledField(stringResource(R.string.aiset_agent_key), agentKey, { agentKey = it }, Modifier, false, null, KeyboardType.Password, true)
        LabeledField(stringResource(R.string.aiset_org), org, { org = it })
        LabeledField(stringResource(R.string.aiset_ws), ws, { ws = it })
        LabeledField(stringResource(R.string.aiset_app), appId, { appId = it })
        LabeledField(stringResource(R.string.aiset_ver), ver, { ver = it.filter(Char::isDigit) }, Modifier, false, null, KeyboardType.Number)
        LabeledField(stringResource(R.string.aiset_conn), conn, { conn = it })
        LabeledField(stringResource(R.string.aiset_phone), phone, { phone = it }, Modifier, false, null, KeyboardType.Phone)

        PrimaryButton(stringResource(R.string.aiset_save), {
            val a = sl.llmStore.save(LlmConfig(base, key, model))
            val b = sl.llmStore.saveSarvam(SarvamConfig(sKey, agentKey, org, ws, appId, ver, conn, phone))
            sl.voice.refreshSarvam()
            saveMsg = (a.exceptionOrNull() ?: b.exceptionOrNull())?.let { ctx.getString(R.string.aiset_store_error, it.message ?: "") }
                ?: ctx.getString(R.string.aiset_saved)
        })
        saveMsg?.let { Body(it) }
    }
}
