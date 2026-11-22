package com.simha.app

import com.simha.app.logic.Approvals
import com.simha.app.logic.Decision
import com.simha.app.logic.Rules
import com.simha.app.logic.RulesEngine
import org.junit.Assert.assertEquals
import org.junit.Test

class RulesEngineTest {
    private val min = 60_000L
    private val rules = Rules(inactivityMinutes = 180, checkinWaitMinutes = 5)
    private val family = Approvals(callFamily = true, callHospital = false, record = false)
    private val none = Approvals(callFamily = false, callHospital = false, record = false)
    private val t0 = 1_000_000_000L

    private fun decide(lastActivity: Long, prompt: Long?, now: Long, a: Approvals = family, r: Rules = rules) =
        RulesEngine.decide(lastActivity, prompt, r, a, now)

    @Test
    fun recentActivityDoesNothing() {
        assertEquals(Decision.None, decide(t0, null, t0 + 10 * min))
    }

    @Test
    fun justBelowLimitDoesNothing() {
        assertEquals(Decision.None, decide(t0, null, t0 + 180 * min - 1))
    }

    @Test
    fun atLimitPromptsCheckin() {
        assertEquals(Decision.PromptCheckin, decide(t0, null, t0 + 180 * min))
    }

    @Test
    fun beyondLimitPromptsCheckin() {
        assertEquals(Decision.PromptCheckin, decide(t0, null, t0 + 500 * min))
    }

    @Test
    fun waitingForAnswerDoesNothing() {
        val prompt = t0 + 180 * min
        assertEquals(Decision.None, decide(t0, prompt, prompt + 4 * min))
    }

    @Test
    fun justBeforeWaitEndsDoesNothing() {
        val prompt = t0 + 180 * min
        assertEquals(Decision.None, decide(t0, prompt, prompt + 5 * min - 1))
    }

    @Test
    fun waitElapsedWithApprovalCallsFamily() {
        val prompt = t0 + 180 * min
        assertEquals(Decision.EscalateCallFamily, decide(t0, prompt, prompt + 5 * min))
    }

    @Test
    fun waitElapsedWithoutApprovalAlertsFamily() {
        val prompt = t0 + 180 * min
        assertEquals(Decision.AlertFamily, decide(t0, prompt, prompt + 5 * min, none))
    }

    @Test
    fun activityAfterPromptCancelsEscalation() {
        val prompt = t0 + 180 * min
        val activity = prompt + 2 * min
        assertEquals(Decision.None, decide(activity, prompt, prompt + 30 * min))
    }

    @Test
    fun activityAtSameInstantAsPromptCountsAsAnswer() {
        val prompt = t0 + 180 * min
        assertEquals(Decision.None, decide(prompt, prompt, prompt + 30 * min))
    }

    @Test
    fun newCycleAfterAnswerPromptsAgain() {
        val prompt = t0 + 180 * min
        val activity = prompt + 1 * min
        assertEquals(Decision.PromptCheckin, decide(activity, prompt, activity + 180 * min))
    }

    @Test
    fun hospitalAndRecordApprovalsDoNotChangeDecision() {
        val prompt = t0 + 180 * min
        val a = Approvals(callFamily = false, callHospital = true, record = true)
        assertEquals(Decision.AlertFamily, decide(t0, prompt, prompt + 6 * min, a))
    }

    @Test
    fun zeroOrNegativeRulesAreClampedToOneMinute() {
        val r = Rules(0, -3)
        assertEquals(Decision.None, decide(t0, null, t0 + 1 * min - 1, family, r))
        assertEquals(Decision.PromptCheckin, decide(t0, null, t0 + 1 * min, family, r))
        val prompt = t0 + 1 * min
        assertEquals(Decision.None, decide(t0, prompt, prompt + 1 * min - 1, family, r))
        assertEquals(Decision.EscalateCallFamily, decide(t0, prompt, prompt + 1 * min, family, r))
    }

    @Test
    fun clockGoingBackwardsDoesNothing() {
        assertEquals(Decision.None, decide(t0, null, t0 - 5 * min))
    }

    @Test
    fun stalePromptFromBeforeActivityIsIgnored() {
        assertEquals(Decision.None, decide(t0 + 100 * min, t0, t0 + 110 * min))
    }

    @Test
    fun customLimitsAreHonoured() {
        val r = Rules(inactivityMinutes = 15, checkinWaitMinutes = 2)
        assertEquals(Decision.PromptCheckin, decide(t0, null, t0 + 15 * min, family, r))
        val prompt = t0 + 15 * min
        assertEquals(Decision.None, decide(t0, prompt, prompt + 1 * min, family, r))
        assertEquals(Decision.EscalateCallFamily, decide(t0, prompt, prompt + 2 * min, family, r))
    }

    @Test
    fun neverActiveBeforeFirstTickPromptsOnlyAfterLimit() {
        assertEquals(Decision.None, decide(0L, null, 179 * min))
        assertEquals(Decision.PromptCheckin, decide(0L, null, 180 * min))
    }
}
