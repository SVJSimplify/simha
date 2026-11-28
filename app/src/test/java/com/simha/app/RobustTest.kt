package com.simha.app

import com.simha.app.care.Clock
import com.simha.app.care.Slots
import com.simha.app.care.TaskType
import com.simha.app.llm.Compat
import com.simha.app.llm.Msg
import com.simha.app.llm.SimhaTools
import com.simha.app.llm.Speakable
import com.simha.app.llm.TextTools
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class RobustTest {
    private val ten = LocalTime.of(10, 0)
    private fun t(s: String) = Clock.resolve(s, ten)

    @Test
    fun indianTimePhrases() {
        assertEquals("17:00", t("5 baje"))
        assertEquals("17:00", t("शाम 5 बजे"))
        assertEquals("08:00", t("सुबह 8 बजे"))
        assertEquals("20:30", t("साढ़े 8 बजे"))
        assertEquals("17:30", t("half past 5"))
        assertEquals("20:45", t("quarter to 9 at night"))
        assertEquals("22:00", t("raat 10"))
        assertEquals("00:00", t("रात 12 बजे"))
        assertEquals("08:00", t("eight in the morning"))
        assertEquals("17:00", t("five"))
        assertEquals("17:00", t("५ बजे"))
        assertEquals("17:00", t("மாலை 5 மணிக்கு"))
        assertEquals("14:00", t("दो बजे"))
        assertEquals("14:00", t("दोपहर 2 बजे"))
        assertEquals("10:10", t("in 10 minutes"))
        assertEquals("10:10", t("10 मिनट बाद"))
        assertEquals(listOf("08:00", "20:00"), Clock.resolveAll("8 am and 8 pm", ten))
        assertEquals(setOf("08:00", "20:00"), Clock.resolveAll("8 and 8", ten).toSet())
    }

    @Test
    fun slotFillingInCode() {
        assertEquals("17:00", Slots.value("time", "at 5 in the evening", ten))
        assertEquals("Amlodipine", Slots.value("name", "Amlodipine.", ten))
        assertTrue(Slots.isCancel("never mind"))
        assertTrue(Slots.isCancel("रहने दो"))
        assertFalse(Slots.isCancel("Amlodipine"))
        assertTrue(Slots.otherIntent("call my son", TaskType.Medicine))
        assertFalse(Slots.otherIntent("Amlodipine", TaskType.Medicine))
    }

    @Test
    fun textToolCalls() {
        val k = SimhaTools.allNames
        val a = TextTools.parse("<tool>{\"name\":\"note_task\",\"arguments\":{\"type\":\"medicine\"}}</tool>", k)
        assertEquals("note_task", a.single().name)
        assertTrue(a[0].arguments.contains("medicine"))
        val b = TextTools.parse("```json\n{\"name\": \"call_contact\", \"parameters\": {\"who\": \"son\"}}\n```", k)
        assertEquals("call_contact", b.single().name)
        val c = TextTools.parse("call_contact(who='Ravi')", k)
        assertEquals("{\"who\":\"Ravi\"}", c.single().arguments)
        assertTrue(TextTools.parse("Hello there, how are you?", k).isEmpty())
        assertTrue(TextTools.parse("{\"name\":\"rm_rf\",\"arguments\":{}}", k).isEmpty())
    }

    @Test
    fun neverSpeaksMachineText() {
        assertEquals("Hello.", Speakable.of("<think>hmm</think>Hello. <tool>{\"name\":\"x\"}</tool>").trim())
        assertEquals("", Speakable.of("{\"name\":\"note_task\"}"))
        assertEquals("", Speakable.of("<think>still thinking"))
        assertEquals("Hi there", Speakable.clean("**Hi** 😊 there"))
        assertEquals("Take rest", Speakable.clean("- Take rest"))
    }

    private fun role(o: kotlinx.serialization.json.JsonElement) = o.jsonObject["role"]!!.jsonPrimitive.content

    @Test
    fun compatTransforms() {
        val msgs = JsonArray(listOf(Msg.system("Be kind"), Msg.assistant("Hi"), Msg.user("hello")))
        val ns = Compat.noSystem(msgs)
        assertTrue(ns.none { role(it) == "system" })
        assertTrue(ns.any { it.jsonObject["content"]!!.jsonPrimitive.content.contains("Be kind") })
        val calls = listOf(com.simha.app.llm.ToolCall("c1", "get_status", "{}"))
        val withTool = JsonArray(listOf(Msg.system("S"), Msg.user("u"), Msg.assistantCalls(null, calls), Msg.tool("c1", "{\"ok\":1}")))
        val nt = Compat.noTools(withTool, SimhaTools.definitionsFor(listOf("get_status")))
        assertTrue(nt.none { role(it) == "tool" })
        assertTrue(nt.first().jsonObject["content"]!!.jsonPrimitive.content.contains("get_status"))
        assertTrue(nt.last().jsonObject["content"]!!.jsonPrimitive.content.contains("Result of get_status"))
        val alt = Compat.alternate(JsonArray(listOf(Msg.user("a"), Msg.user("b"), Msg.assistant("c"))))
        assertEquals(2, alt.size)
    }
}
