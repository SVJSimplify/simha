package com.simha.app.ui.home

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.util.UUID

class MsgRecorder(private val ctx: Context) {
    private var rec: MediaRecorder? = null
    private var player: MediaPlayer? = null
    var file: File? = null
        private set

    @Suppress("DEPRECATION")
    private fun newRecorder(): MediaRecorder = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(ctx) else MediaRecorder()

    fun start(onMax: () -> Unit): Result<Unit> {
        discard()
        val f = File(ctx.cacheDir, "msg_" + UUID.randomUUID() + ".m4a")
        return try {
            val r = newRecorder()
            rec = r
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setMaxDuration(MAX_MS)
            r.setOutputFile(f.absolutePath)
            r.setOnInfoListener { _, what, _ -> if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) onMax() }
            r.prepare()
            r.start()
            file = f
            Result.success(Unit)
        } catch (e: Exception) {
            f.delete()
            releaseRec()
            Result.failure(e)
        }
    }

    fun stop(): Boolean {
        val r = rec ?: return false
        rec = null
        return try {
            r.stop()
            true
        } catch (e: Exception) {
            file?.delete()
            file = null
            false
        } finally {
            r.release()
        }
    }

    fun play(onDone: () -> Unit): Boolean {
        val f = file ?: return false
        stopPlay()
        return try {
            val p = MediaPlayer()
            player = p
            p.setDataSource(f.absolutePath)
            p.setOnCompletionListener { stopPlay(); onDone() }
            p.prepare()
            p.start()
            true
        } catch (e: Exception) {
            stopPlay()
            false
        }
    }

    fun stopPlay() {
        player?.let {
            try {
                it.release()
            } catch (e: Exception) {
            }
        }
        player = null
    }

    private fun releaseRec() {
        rec?.let {
            try {
                it.release()
            } catch (e: Exception) {
            }
        }
        rec = null
    }

    fun discard() {
        stopPlay()
        releaseRec()
        file?.delete()
        file = null
    }

    companion object {
        const val MAX_MS = 60_000
    }
}
