package com.simha.app.wake

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.app.NotificationManagerCompat
import com.simha.app.LocaleCtx
import com.simha.app.SimhaApp
import com.simha.app.ui.talk.ConversationPanel
import com.simha.app.ui.theme.SimhaTheme
import kotlinx.coroutines.delay

class WakeActivity : AppCompatActivity() {
    private var req by mutableStateOf<WakeRequest?>(null)
    private var done = false

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleCtx.wrapped(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        volumeControlStream = AudioManager.STREAM_ALARM
        load(intent)
        setContent {
            SimhaTheme(dark = isSystemInDarkTheme(), lang = com.simha.app.LocaleCtx.saved(this)) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val r = req
                    if (r != null) {
                        key(r.id, r.attempt) {
                            LaunchedEffect(r.id, r.attempt) {
                                delay(SESSION_MS)
                                finishSession()
                            }
                            ConversationPanel(reason = r.reason, extra = r.extra, onDone = { finishSession() })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        done = false
        load(intent)
    }

    private fun load(i: Intent?) {
        val r = WakeRequest.parse(i?.getStringExtra(Wake.X_REQ)) ?: Wake.pending(applicationContext)
        if (r == null) {
            done = true
            finish()
            return
        }
        NotificationManagerCompat.from(applicationContext).cancel(NOTIFICATION_ID)
        (applicationContext as SimhaApp).sl.voice.stopSpeaking()
        WakeAudio.begin(applicationContext, r.attempt)
        req = r
    }

    private fun finishSession() {
        if (done) return
        done = true
        if (Wake.hasReplied()) Wake.resolve(applicationContext) else Wake.afterSilence(applicationContext)
        finish()
    }

    override fun onDestroy() {
        if (!done && !isChangingConfigurations) {
            done = true
            if (Wake.hasReplied()) Wake.resolve(applicationContext) else Wake.afterSilence(applicationContext)
        }
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 21
        private const val SESSION_MS = 4L * 60_000L
    }
}
