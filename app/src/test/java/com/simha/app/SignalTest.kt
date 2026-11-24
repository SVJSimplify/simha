package com.simha.app

import com.simha.app.logic.Fact
import com.simha.app.logic.Facts
import com.simha.app.logic.Sig
import com.simha.app.logic.Signal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SignalTest {
    private val full = Sig(
        mood = 4, taken = 2, due = 3, pain = mapOf("knee" to 2), sleep = "p", water = "lo", activity = "ok",
        visitors = 1, ciOk = 3, ciMiss = 0, note = "slept badly",
    )

    @Test
    fun encodeDecodeRoundTrip() {
        val t = Signal.encode(full)
        assertEquals("m4 md2/3 p:knee2 s:p w:lo a:ok v:1 ci:3/0 n:\"slept badly\"", t)
        assertEquals(full, Signal.decode(t))
        val x = full.copy(exc = setOf("redflag", "fall"))
        assertEquals("m4 md2/3 p:knee2 s:p w:lo a:ok v:1 ci:3/0 x:fall,redflag n:\"slept badly\"", Signal.encode(x))
        assertEquals(x, Signal.decode(Signal.encode(x)))
    }

    @Test
    fun decodeSpecExampleAndGarbage() {
        val d = Signal.decode("m4 md2/3 p:knee2 s:p w:lo a:ok v:1 ci:3/0 x:- n:\"a b \"c\"\"")
        assertEquals(4, d.mood)
        assertTrue(d.exc.isEmpty())
        assertEquals("a b 'c'", d.note)
        assertEquals(Sig(), Signal.decode(""))
        assertEquals(Sig(), Signal.decode("m9 s:zz w:x a:q md/ ci:1 junk"))
        assertEquals(40, Signal.cleanNote("x".repeat(100))!!.length)
    }

    @Test
    fun mergeKeepsAndOverrides() {
        val a = Sig(mood = 3, pain = mapOf("knee" to 1), exc = setOf("fall"), visitors = 1)
        val b = Sig(mood = 5, pain = mapOf("back" to 2, "knee" to 3), exc = setOf("esc"), sleep = "g")
        val m = Signal.merge(a, b)
        assertEquals(5, m.mood)
        assertEquals(mapOf("knee" to 3, "back" to 2), m.pain)
        assertEquals(setOf("fall", "esc"), m.exc)
        assertEquals(1, m.visitors)
        assertEquals("g", m.sleep)
        assertEquals(a, Signal.merge(a, Sig()))
    }

    @Test
    fun weeklyRollup() {
        val days = listOf(
            Sig(mood = 4, taken = 3, due = 3, pain = mapOf("knee" to 2)),
            Sig(mood = 3, taken = 2, due = 3, ciMiss = 1),
            Sig(mood = 4, taken = 3, due = 4, pain = mapOf("knee" to 1)),
        )
        assertEquals("md:80% m:3.7 ci_miss:1 falls:0 pain:knee", Signal.rollup(days))
        assertEquals("ci_miss:0 falls:1", Signal.rollup(listOf(Sig(exc = setOf("fall")))))
        val r = Signal.parseRollup("md:90% m:3.6 ci_miss:1 falls:0 pain:knee")
        assertEquals(90, r.mdPct)
        assertEquals(3.6, r.mood!!, 0.001)
        assertEquals(1, r.ciMiss)
        assertEquals(listOf("knee"), r.pain)
    }

    @Test
    fun summaryAndWeekKey() {
        assertTrue(Signal.summary(full).contains("pain knee 2"))
        assertEquals("2026-W48", Signal.weekKey(LocalDate.of(2026, 11, 24)))
        assertEquals("2026-W01", Signal.weekKey(LocalDate.of(2025, 12, 29)))
    }

    @Test
    fun factsUpsertDedupeAndEvict() {
        assertEquals("person:priya_is_the_daughter", Facts.keyFor("person", " Priya is the daughter "))
        assertEquals("asked:medicines", Facts.keyFor("fact", "asked:medicines"))
        val r = Facts.upsert(emptyList(), "Fact:Knee Pain", "knee sore", 2, 1)
        assertTrue(r.changed)
        val l0 = r.facts
        assertEquals("fact:knee_pain", l0[0].k)
        assertFalse(Facts.upsert(l0, "fact:knee_pain", "knee sore", 2, 5).changed)
        assertEquals(1, Facts.upsert(l0, "fact:knee_pain", "knee better", 1, 5).facts.size)
        assertEquals(2, Facts.upsert(l0, "fact:knee_pain", "knee better", 1, 5).facts[0].w)
        val l = (1..Facts.MAX).fold(emptyList<Fact>()) { acc, i -> Facts.upsert(acc, "fact:k$i", "v", if (i == 1) 1 else 3, i.toLong()).facts }
        assertEquals(Facts.MAX, l.size)
        val o = Facts.upsert(l, "fact:new", "v", 1, 100)
        assertEquals(Facts.MAX, o.facts.size)
        assertEquals(listOf("fact:k1"), o.evicted)
        val o2 = Facts.upsert(o.facts, "fact:newer", "v", 3, 200)
        assertEquals(listOf("fact:new"), o2.evicted)
        val o3 = Facts.upsert(o2.facts, "fact:newest", "v", 3, 300)
        assertEquals(listOf("fact:k2"), o3.evicted)
    }
}
