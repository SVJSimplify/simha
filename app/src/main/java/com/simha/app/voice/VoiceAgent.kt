package com.simha.app.voice

import com.simha.app.llm.LlmStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AgentCall(
    val personName: String,
    val contactName: String,
    val number: String,
    val minutes: Int,
    val langName: String,
    val firstLine: String,
)

interface VoiceAgent {
    suspend fun call(req: AgentCall): Result<String>
}

class AgentNotConfiguredException : Exception("Voice agent calls are not set up")

class SarvamVoiceAgent(
    private val store: LlmStore,
    private val client: SarvamClient = SarvamClient(),
) : VoiceAgent {
    override suspend fun call(req: AgentCall): Result<String> {
        val c = withContext(Dispatchers.IO) { store.sarvam() }
        if (!c.agentReady) return Result.failure(AgentNotConfiguredException())
        return client.outbound(c, req)
    }
}
