package com.simha.app.agenda

import android.content.Context

object AgendaStore {
    private fun p(ctx: Context) = ctx.getSharedPreferences("agenda", Context.MODE_PRIVATE)
    fun lastFired(ctx: Context): String? = p(ctx).getString("last", null)?.takeIf { System.currentTimeMillis() - p(ctx).getLong("last_at", 0L) < 3 * 3_600_000L }
    fun fired(ctx: Context, what: String) = p(ctx).edit().putString("last", what).putLong("last_at", System.currentTimeMillis()).apply()
    fun codes(ctx: Context): Set<String> = p(ctx).getStringSet("codes", emptySet()) ?: emptySet()
    fun setCodes(ctx: Context, c: Set<String>) = p(ctx).edit().putStringSet("codes", c).apply()
    fun code(id: String): Int = 8000 + Math.floorMod(id.hashCode(), 900)
}
