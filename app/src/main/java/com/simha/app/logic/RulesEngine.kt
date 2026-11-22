package com.simha.app.logic

data class Rules(val inactivityMinutes: Int, val checkinWaitMinutes: Int)

data class Approvals(val callFamily: Boolean, val callHospital: Boolean, val record: Boolean)

sealed interface Decision {
    data object None : Decision
    data object PromptCheckin : Decision
    data object EscalateCallFamily : Decision
    data object AlertFamily : Decision
}

object RulesEngine {
    private const val MINUTE = 60_000L

    fun decide(
        lastActivity: Long,
        lastCheckinPrompt: Long?,
        rules: Rules,
        approvals: Approvals,
        now: Long,
    ): Decision {
        val inactivity = rules.inactivityMinutes.coerceAtLeast(1) * MINUTE
        val wait = rules.checkinWaitMinutes.coerceAtLeast(1) * MINUTE
        val awaitingAnswer = lastCheckinPrompt != null && lastCheckinPrompt > lastActivity
        if (!awaitingAnswer) {
            return if (now - lastActivity >= inactivity) Decision.PromptCheckin else Decision.None
        }
        if (now - lastCheckinPrompt!! < wait) return Decision.None
        return if (approvals.callFamily) Decision.EscalateCallFamily else Decision.AlertFamily
    }
}
