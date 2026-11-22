package com.simha.app.voice

interface VoiceAgent {
    suspend fun call(purpose: String, number: String): Result<String>
}

class AgentNotConfiguredException : Exception("Voice agent calls are not set up in this build")

class SarvamVoiceAgent : VoiceAgent {
    override suspend fun call(purpose: String, number: String): Result<String> =
        Result.failure(AgentNotConfiguredException())
}
