package com.simha.app.voice

import android.content.Context
import android.media.AudioManager
import com.simha.app.Actions
import com.simha.app.R
import com.simha.app.SimhaApp
import com.simha.app.data.CallResult
import com.simha.app.data.ContactEntity
import com.simha.app.data.Snap
import com.simha.app.data.jo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object Escalation {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun callFamily(ctx: Context, lc: Context, p: Snap, target: ContactEntity, minutes: Int): Boolean {
        if (!p.callFamily || !target.approved) return false
        val sl = (ctx.applicationContext as SimhaApp).sl
        val line = lc.getString(R.string.esc_line, p.name, minutes)
        val r = SarvamVoiceAgent(sl.llmStore).call(AgentCall(p.name, target.name, target.phone, minutes, Langs.name(p.lang), line))
        if (r.isSuccess) {
            sl.repo.logCall("approved", target.phone, "escalation_voice_agent", "agent_started")
            sl.repo.audit("escalation_voice_agent", jo("attempt_id" to r.getOrNull().orEmpty(), "contact" to target.name))
            return true
        }
        sl.repo.audit(
            "escalation_voice_agent_unavailable",
            jo("reason" to (r.exceptionOrNull()?.message ?: "unknown"), "fallback" to "approved_call_with_speakerphone_tts"),
        )
        if (!Actions.has(ctx, android.Manifest.permission.CALL_PHONE)) return false
        if (sl.repo.placeCall(target.phone, true, "escalation_speakerphone") == CallResult.Called) {
            scope.launch {
                val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                delay(10_000)
                am.isSpeakerphoneOn = true
                repeat(2) {
                    sl.voice.say(line, p.lang)
                    delay(2_000)
                }
                sl.repo.audit("escalation_speakerphone_spoken", jo("contact" to target.name))
            }
        }
        return false
    }
}
