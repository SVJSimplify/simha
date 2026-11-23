package com.simha.app.monitor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.simha.app.SimhaApp
import com.simha.app.data.jo
import com.simha.app.wake.Wake
import com.simha.app.wake.WakeReason
import com.simha.app.wake.WakeStore
import kotlin.math.abs
import kotlin.math.sqrt

class FallMachine {
    private enum class Phase { Idle, Free, Impact }

    private var phase = Phase.Idle
    private var lowAt = 0L
    private var hitAt = 0L
    private var lastT = 0L

    fun reset() {
        phase = Phase.Idle
        lowAt = 0L
        hitAt = 0L
    }

    fun feed(t: Long, g: Float): Boolean {
        if (lastT != 0L && t - lastT > GAP_MS) reset()
        lastT = t
        when (phase) {
            Phase.Idle -> if (g < FREE_G) {
                phase = Phase.Free
                lowAt = t
            }
            Phase.Free -> when {
                g < FREE_G -> lowAt = t
                g > IMPACT_G && t - lowAt <= WINDOW_MS -> {
                    phase = Phase.Impact
                    hitAt = t
                }
                t - lowAt > WINDOW_MS -> reset()
            }
            Phase.Impact -> {
                val dt = t - hitAt
                if (dt >= SETTLE_MS && abs(g - 1f) > MOVE_G) {
                    reset()
                } else if (dt >= STILL_MS) {
                    reset()
                    return true
                }
            }
        }
        return false
    }

    companion object {
        const val FREE_G = 0.5f
        const val IMPACT_G = 2.5f
        const val MOVE_G = 0.35f
        const val WINDOW_MS = 1000L
        const val SETTLE_MS = 1500L
        const val STILL_MS = 10_000L
        const val GAP_MS = 3000L
    }
}

class FallDetector(private val ctx: Context, private val onFall: () -> Unit) : SensorEventListener {
    private val machine = FallMachine()
    private var thread: HandlerThread? = null
    private var sm: SensorManager? = null

    fun start() {
        if (thread != null) return
        val m = ctx.getSystemService(SensorManager::class.java) ?: return
        val s = m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER, true) ?: m.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
        val t = HandlerThread("simha-fall").also { it.start() }
        thread = t
        sm = m
        if (!m.registerListener(this, s, SensorManager.SENSOR_DELAY_GAME, Handler(t.looper))) {
            Log.w("Simha", "fall sensor registration failed")
            stop()
        }
    }

    fun stop() {
        sm?.unregisterListener(this)
        sm = null
        thread?.quitSafely()
        thread = null
        machine.reset()
    }

    override fun onSensorChanged(e: SensorEvent) {
        val g = sqrt(e.values[0] * e.values[0] + e.values[1] * e.values[1] + e.values[2] * e.values[2]) / SensorManager.GRAVITY_EARTH
        if (machine.feed(e.timestamp / 1_000_000L, g)) onFall()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        private const val DEBOUNCE_MS = 10L * 60_000L

        suspend fun handle(ctx: Context) {
            val app = ctx.applicationContext
            val sl = (app as SimhaApp).sl
            val p = sl.prefs.snap()
            if (p.role != "self" || !p.setup || !p.running || !Wake.ui(app).fall) return
            val now = System.currentTimeMillis()
            if (now - WakeStore.long(app, "fall_last") < DEBOUNCE_MS) return
            WakeStore.put(app, "fall_last" to now)
            sl.repo.audit("fall_detected")
            sl.repo.logEvent("fall_suspected", jo("source" to "accelerometer"))
            Wake.fire(app, com.simha.app.wake.WakeRequest(WakeReason.Fall, "A hard fall was detected followed by stillness. Ask if the user is hurt."))
        }
    }
}
