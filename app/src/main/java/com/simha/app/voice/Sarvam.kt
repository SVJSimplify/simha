package com.simha.app.voice

import com.simha.app.llm.SarvamConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.ByteArrayOutputStream
import java.util.Base64

class SarvamException(message: String) : Exception(message)

object Sarvam {
    const val STT_URL = "https://api.sarvam.ai/speech-to-text"
    const val TTS_URL = "https://api.sarvam.ai/text-to-speech"
    const val STT_MODEL = "saaras:v4"
    const val TTS_MODEL = "bulbul:v3"
    const val SPEAKER = "priya"
    const val MAX_TTS_CHARS = 2500

    fun ttsBody(text: String, langTag: String): String = buildJsonObject {
        put("text", text.take(MAX_TTS_CHARS))
        put("language_code", langTag)
        put("model", TTS_MODEL)
        put("speaker", SPEAKER)
        put("output_audio_codec", "wav")
    }.toString()

    fun parseTts(body: String): ByteArray {
        val o = try {
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw SarvamException("Reply was not valid JSON")
        }
        val a = o["audios"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.contentOrNull
        if (a.isNullOrEmpty()) throw SarvamException("Reply had no audio")
        return try {
            Base64.getMimeDecoder().decode(a)
        } catch (e: IllegalArgumentException) {
            throw SarvamException("Audio was not valid base64")
        }
    }

    fun parseStt(body: String): String {
        val o = try {
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw SarvamException("Reply was not valid JSON")
        }
        return o["transcript"]?.jsonPrimitive?.contentOrNull?.trim() ?: throw SarvamException("Reply had no transcript")
    }

    fun errorText(status: Int, body: String): String {
        val detail = try {
            val o = Json.parseToJsonElement(body).jsonObject
            val e = o["error"]
            if (e is JsonObject) e["message"]?.jsonPrimitive?.contentOrNull
            else o["message"]?.jsonPrimitive?.contentOrNull ?: o["detail"]?.toString()
        } catch (e: Exception) {
            null
        }
        return "HTTP " + status + ": " + (detail ?: body.take(200)).take(300)
    }

    fun e164(phone: String): String {
        val d = phone.filter { it.isDigit() }
        return when {
            phone.trim().startsWith("+") -> "+$d"
            d.length == 10 -> "+91$d"
            d.length == 12 && d.startsWith("91") -> "+$d"
            d.length == 11 && d.startsWith("0") -> "+91" + d.drop(1)
            else -> "+$d"
        }
    }

    fun outboundUrl(c: SarvamConfig): String =
        "https://apps.sarvam.ai/api/outbounds/v1/orgs/" + c.orgId.trim() + "/workspaces/" + c.workspaceId.trim() + "/outbounds"

    fun outboundBody(c: SarvamConfig, userPhone: String, firstLine: String, langName: String, vars: Map<String, String>): String {
        val ver = c.appVersion.trim().toIntOrNull() ?: throw SarvamException("App version must be a number")
        return buildJsonObject {
            putJsonObject("app_config") {
                put("app_id", c.appId.trim())
                put("app_version", ver)
                putJsonObject("connection_config") {
                    put("connection_id", c.connectionId.trim())
                    put("agent_phone_number", c.agentPhone.trim())
                }
                putJsonObject("agent_variables") { vars.forEach { (k, v) -> put(k, JsonPrimitive(v)) } }
                putJsonObject("app_overrides") {
                    put("initial_bot_message", firstLine)
                    put("initial_language_name", langName)
                }
            }
            putJsonObject("user_config") { put("user_phone_number", e164(userPhone)) }
        }.toString()
    }

    fun parseAttempt(body: String): String {
        val o = try {
            Json.parseToJsonElement(body).jsonObject
        } catch (e: Exception) {
            throw SarvamException("Reply was not valid JSON")
        }
        return o["attempt_id"]?.jsonPrimitive?.contentOrNull ?: throw SarvamException("Reply had no attempt id")
    }
}

object Wav {
    fun encode(pcm: ByteArray, rate: Int): ByteArray {
        val out = ByteArrayOutputStream(44 + pcm.size)
        fun i32(v: Int) {
            out.write(v and 0xff); out.write((v shr 8) and 0xff); out.write((v shr 16) and 0xff); out.write((v shr 24) and 0xff)
        }
        fun i16(v: Int) {
            out.write(v and 0xff); out.write((v shr 8) and 0xff)
        }
        out.write("RIFF".toByteArray())
        i32(36 + pcm.size)
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        i32(16); i16(1); i16(1); i32(rate); i32(rate * 2); i16(2); i16(16)
        out.write("data".toByteArray())
        i32(pcm.size)
        out.write(pcm)
        return out.toByteArray()
    }

    fun rms(buf: ShortArray, n: Int): Double {
        if (n <= 0) return 0.0
        var s = 0.0
        for (i in 0 until n) s += buf[i].toDouble() * buf[i]
        return Math.sqrt(s / n)
    }
}

enum class EpState { Waiting, Done, Timeout }

class Endpointer(
    private val frameMs: Int = 30,
    private val calibrateFrames: Int = 8,
    private val silenceMs: Int = 1200,
    private val noSpeechMs: Int = 7000,
    private val maxMs: Int = 25000,
    private val minThreshold: Double = 350.0,
) {
    private var frames = 0
    private var floorSum = 0.0
    private var threshold = minThreshold
    private var speechFrames = 0
    private var silentRun = 0

    val heardSpeech: Boolean get() = speechFrames >= 3

    fun feed(rms: Double): EpState {
        frames++
        if (frames <= calibrateFrames) {
            floorSum += rms
            if (frames == calibrateFrames) threshold = maxOf(minThreshold, floorSum / calibrateFrames * 2.5)
            return EpState.Waiting
        }
        if (rms > threshold) {
            speechFrames++
            silentRun = 0
        } else {
            silentRun++
        }
        val elapsed = frames * frameMs
        if (!heardSpeech) return if (elapsed >= noSpeechMs) EpState.Timeout else EpState.Waiting
        if (silentRun * frameMs >= silenceMs || elapsed >= maxMs) return EpState.Done
        return EpState.Waiting
    }
}
