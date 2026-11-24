package com.simha.app

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleCtx {
    private const val P = "lang_mirror"
    private const val K_LANG = "lang"
    private const val K_CLEARED = "cleared"
    private val SUPPORTED = setOf("en", "hi", "te", "kn", "ta")

    fun wrap(base: Context, tag: String): Context {
        val loc = Locale.forLanguageTag(tag)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(loc)
        cfg.setLayoutDirection(loc)
        return base.createConfigurationContext(cfg)
    }

    fun saved(ctx: Context): String =
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).getString(K_LANG, null)
            ?: Locale.getDefault().language.takeIf { it in SUPPORTED } ?: "en"

    fun save(ctx: Context, tag: String) {
        ctx.getSharedPreferences(P, Context.MODE_PRIVATE).edit().putString(K_LANG, tag).apply()
    }

    fun cleared(ctx: Context): Boolean {
        val sp = ctx.getSharedPreferences(P, Context.MODE_PRIVATE)
        if (sp.getBoolean(K_CLEARED, false)) return true
        sp.edit().putBoolean(K_CLEARED, true).apply()
        return false
    }

    fun wrapped(base: Context): Context = wrap(base, saved(base))
}

class LocalizedContext(base: Context, private val res: Resources) : ContextWrapper(base) {
    override fun getResources(): Resources = res
}
