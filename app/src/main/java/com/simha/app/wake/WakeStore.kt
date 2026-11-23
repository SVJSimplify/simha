package com.simha.app.wake

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

object WakeStore {
    private fun sp(c: Context): SharedPreferences = c.applicationContext.getSharedPreferences("simha_wake", Context.MODE_PRIVATE)

    fun str(c: Context, k: String, d: String? = null): String? = sp(c).getString(k, d)

    fun long(c: Context, k: String, d: Long = 0L): Long = sp(c).getLong(k, d)

    fun int(c: Context, k: String, d: Int = 0): Int = sp(c).getInt(k, d)

    fun bool(c: Context, k: String, d: Boolean = false): Boolean = sp(c).getBoolean(k, d)

    fun set(c: Context, k: String): Set<String> = sp(c).getStringSet(k, emptySet()) ?: emptySet()

    fun put(c: Context, vararg kv: Pair<String, Any?>) {
        val e = sp(c).edit()
        for ((k, v) in kv) {
            when (v) {
                null -> e.remove(k)
                is String -> e.putString(k, v)
                is Long -> e.putLong(k, v)
                is Int -> e.putInt(k, v)
                is Boolean -> e.putBoolean(k, v)
                is Set<*> -> e.putStringSet(k, v.filterIsInstance<String>().toSet())
            }
        }
        e.apply()
    }
}

object RoutineStore {
    private const val KEY = "routine_ts"
    private const val BUCKET = 600_000L
    private const val KEEP_MS = 15L * 86_400_000L

    fun load(ctx: Context): List<Long> = WakeStore.str(ctx, KEY, "").orEmpty().split(",").mapNotNull { it.toLongOrNull() }

    fun record(ctx: Context, ts: Long, now: Long) {
        if (ts <= 0L || ts > now + BUCKET) return
        val b = ts / BUCKET * BUCKET
        val l = load(ctx)
        if (l.isNotEmpty() && b <= l.last()) return
        WakeStore.put(ctx, KEY to (l.filter { it >= now - KEEP_MS } + b).joinToString(","))
    }
}

data class WakeRequest(
    val reason: WakeReason,
    val extra: String? = null,
    val attempt: Int = 1,
    val id: Long = 0L,
    val medIds: List<String> = emptyList(),
    val scheduledAt: Long = 0L,
) {
    fun toJson(): String = JSONObject()
        .put("reason", reason.name)
        .put("extra", extra ?: "")
        .put("attempt", attempt)
        .put("id", id)
        .put("meds", JSONArray(medIds))
        .put("at", scheduledAt)
        .toString()

    companion object {
        fun parse(s: String?): WakeRequest? {
            if (s.isNullOrBlank()) return null
            return try {
                val o = JSONObject(s)
                val m = o.optJSONArray("meds") ?: JSONArray()
                WakeRequest(
                    WakeReason.valueOf(o.getString("reason")),
                    o.optString("extra").ifBlank { null },
                    o.optInt("attempt", 1),
                    o.optLong("id", 0L),
                    (0 until m.length()).map { m.getString(it) },
                    o.optLong("at", 0L),
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
