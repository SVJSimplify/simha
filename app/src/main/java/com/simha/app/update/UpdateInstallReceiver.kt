package com.simha.app.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import com.simha.app.LocaleCtx
import com.simha.app.R

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(base: Context, intent: Intent) {
        val ctx = LocaleCtx.wrapped(base)
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm == null) {
                    UpdateState.fail(ctx.getString(R.string.upd_err_install, ""))
                    return
                }
                UpdateState.confirm(confirm)
            }
            PackageInstaller.STATUS_SUCCESS -> UpdateState.installed()
            else -> {
                val code = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
                UpdateState.fail(ctx.getString(R.string.upd_err_install, "$msg ($code)".trim()))
            }
        }
    }
}
