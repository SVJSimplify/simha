package com.simha.app.update

import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.simha.app.BuildConfig
import com.simha.app.LocaleCtx
import com.simha.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UpdateUi(
    val info: UpdateInfo? = null,
    val checking: Boolean = false,
    val busy: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
    val installed: Boolean = false,
    val pendingConfirm: Intent? = null,
)

object UpdateState {
    private const val PREFS = "update"
    private const val KEY_LAST = "last_check"
    private const val EVERY_MS = 6L * 60 * 60 * 1000
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _ui = MutableStateFlow(UpdateUi())
    val ui: StateFlow<UpdateUi> = _ui.asStateFlow()
    private var app: Context? = null

    fun attach(ctx: Context) {
        if (app == null) {
            app = ctx.applicationContext
            val c = ctx.applicationContext
            if (c.getSharedPreferences("update", Context.MODE_PRIVATE).getInt("notified", 0) <= BuildConfig.VERSION_CODE) clearNotif(c)
        }
    }

    private fun clearNotif(c: Context) {
        NotificationManagerCompat.from(c).cancel(UpdateWorker.NOTIF_ID)
    }

    fun checkNow() {
        val c = app ?: return
        if (_ui.value.checking || _ui.value.busy) return
        _ui.update { it.copy(checking = true, error = null) }
        scope.launch {
            val r = Updater.check(c)
            c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
            if (r.isSuccess && r.getOrNull() == null) clearNotif(c)
            _ui.update {
                it.copy(
                    checking = false,
                    info = r.getOrNull(),
                    error = r.exceptionOrNull()?.let { e -> e.message ?: c.getString(R.string.upd_err_parse) },
                )
            }
        }
    }

    fun checkIfDue() {
        val c = app ?: return
        val last = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST, 0L)
        if (System.currentTimeMillis() - last >= EVERY_MS) checkNow()
    }

    fun install() {
        val c = app ?: return
        val info = _ui.value.info ?: return
        if (_ui.value.busy) return
        _ui.update { it.copy(busy = true, progress = 0f, error = null, installed = false, pendingConfirm = null) }
        scope.launch {
            val r = Updater.downloadAndInstall(c, info) { p -> _ui.update { it.copy(progress = p) } }
            val e = r.exceptionOrNull()
            if (e != null) {
                _ui.update { it.copy(busy = false, error = e.message ?: LocaleCtx.wrapped(c).getString(R.string.upd_err_install, "")) }
            }
        }
    }

    fun confirm(intent: Intent) {
        _ui.update { it.copy(pendingConfirm = intent) }
    }

    fun confirmStarted() {
        _ui.update { it.copy(pendingConfirm = null) }
    }

    fun fail(message: String) {
        _ui.update { it.copy(busy = false, error = message, pendingConfirm = null) }
    }

    fun installed() {
        _ui.update { it.copy(busy = false, installed = true, error = null, pendingConfirm = null) }
    }
}
