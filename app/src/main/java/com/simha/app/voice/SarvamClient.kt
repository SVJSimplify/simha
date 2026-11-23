package com.simha.app.voice

import com.simha.app.llm.SarvamConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

class SarvamClient(
    private val http: HttpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 45_000
        }
    },
) {
    private suspend fun <T> guarded(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: SarvamException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(SarvamException("Network error: " + (e.message ?: e.javaClass.simpleName)))
    }

    private suspend fun HttpResponse.checked(): String {
        val t = bodyAsText()
        if (!status.isSuccess()) throw SarvamException(Sarvam.errorText(status.value, t))
        return t
    }

    suspend fun tts(key: String, text: String, langTag: String): Result<ByteArray> = guarded {
        val r = http.post(Sarvam.TTS_URL) {
            header("api-subscription-key", key)
            contentType(ContentType.Application.Json)
            setBody(Sarvam.ttsBody(text, langTag))
        }
        Sarvam.parseTts(r.checked())
    }

    suspend fun stt(key: String, wav: ByteArray, langTag: String): Result<String> = guarded {
        val r = http.submitFormWithBinaryData(
            url = Sarvam.STT_URL,
            formData = formData {
                append("model", Sarvam.STT_MODEL)
                append("mode", "transcribe")
                append("language_code", langTag)
                append(
                    "file", wav,
                    Headers.build {
                        append(HttpHeaders.ContentType, "audio/wav")
                        append(HttpHeaders.ContentDisposition, "filename=\"speech.wav\"")
                    },
                )
            },
        ) { header("api-subscription-key", key) }
        Sarvam.parseStt(r.checked())
    }

    suspend fun outbound(c: SarvamConfig, req: AgentCall): Result<String> = guarded {
        val body = Sarvam.outboundBody(
            c, req.number, req.firstLine, req.langName,
            mapOf(
                "person_name" to req.personName,
                "contact_name" to req.contactName,
                "minutes_since_checkin" to req.minutes.toString(),
            ),
        )
        val r = http.post(Sarvam.outboundUrl(c)) {
            header("X-API-Key", c.agentKey.trim())
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        Sarvam.parseAttempt(r.checked())
    }
}
