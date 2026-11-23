package com.simha.app.llm

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.preparePost
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class LlmConfig(val baseUrl: String = "", val apiKey: String = "", val model: String = "") {
    val configured: Boolean get() = baseUrl.isNotBlank() && apiKey.isNotBlank() && model.isNotBlank()
}

data class SarvamConfig(
    val apiKey: String = "",
    val agentKey: String = "",
    val orgId: String = "",
    val workspaceId: String = "",
    val appId: String = "",
    val appVersion: String = "",
    val connectionId: String = "",
    val agentPhone: String = "",
) {
    val speechReady: Boolean get() = apiKey.isNotBlank()
    val agentReady: Boolean
        get() = listOf(agentKey, orgId, workspaceId, appId, connectionId, agentPhone).all { it.isNotBlank() } && appVersion.trim().toIntOrNull() != null
}

class LlmStore(private val ctx: Context) {
    private val sp by lazy {
        val key = MasterKey.Builder(ctx).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            ctx,
            "llm_secure",
            key,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun load(): Result<LlmConfig> = try {
        Result.success(
            LlmConfig(
                sp.getString("base", "") ?: "",
                sp.getString("key", "") ?: "",
                sp.getString("model", "") ?: "",
            )
        )
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun config(): LlmConfig = load().getOrDefault(LlmConfig())

    fun sarvam(): SarvamConfig = try {
        SarvamConfig(
            sp.getString("s_key", "") ?: "",
            sp.getString("s_agent_key", "") ?: "",
            sp.getString("s_org", "") ?: "",
            sp.getString("s_ws", "") ?: "",
            sp.getString("s_app", "") ?: "",
            sp.getString("s_ver", "") ?: "",
            sp.getString("s_conn", "") ?: "",
            sp.getString("s_phone", "") ?: "",
        )
    } catch (e: Exception) {
        SarvamConfig()
    }

    fun saveSarvam(c: SarvamConfig): Result<Unit> = try {
        sp.edit()
            .putString("s_key", c.apiKey.trim())
            .putString("s_agent_key", c.agentKey.trim())
            .putString("s_org", c.orgId.trim())
            .putString("s_ws", c.workspaceId.trim())
            .putString("s_app", c.appId.trim())
            .putString("s_ver", c.appVersion.trim())
            .putString("s_conn", c.connectionId.trim())
            .putString("s_phone", c.agentPhone.trim())
            .apply()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun save(c: LlmConfig): Result<Unit> = try {
        sp.edit().putString("base", c.baseUrl.trim()).putString("key", c.apiKey.trim()).putString("model", c.model.trim()).apply()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}

class LlmClient(
    private val http: HttpClient = HttpClient(OkHttp) {
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 60_000
        }
    },
) {
    private fun url(c: LlmConfig) = c.baseUrl.trim().trimEnd('/') + "/chat/completions"

    private fun body(c: LlmConfig, messages: JsonArray, tools: JsonArray?, stream: Boolean): String = buildJsonObject {
        put("model", c.model)
        put("messages", messages)
        if (tools != null && tools.isNotEmpty()) {
            put("tools", tools)
            put("tool_choice", "auto")
        }
        if (stream) put("stream", true)
    }.toString()

    private fun HttpRequestBuilder.auth(c: LlmConfig) {
        header(HttpHeaders.Authorization, "Bearer " + c.apiKey)
        if (c.baseUrl.contains("sarvam", ignoreCase = true)) header("api-subscription-key", c.apiKey)
        contentType(ContentType.Application.Json)
    }

    suspend fun complete(c: LlmConfig, messages: JsonArray, tools: JsonArray?): ParsedReply {
        try {
            val r = http.post(url(c)) {
                auth(c)
                setBody(body(c, messages, tools, false))
            }
            val text = r.bodyAsText()
            if (!r.status.isSuccess()) throw LlmException(LlmParse.httpError(r.status.value, text))
            return LlmParse.parse(text)
        } catch (e: LlmException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LlmException("Network error: " + (e.message ?: e.javaClass.simpleName))
        }
    }

    suspend fun stream(c: LlmConfig, messages: JsonArray, tools: JsonArray?, onDelta: (String) -> Unit): ParsedReply {
        try {
            return http.preparePost(url(c)) {
                auth(c)
                setBody(body(c, messages, tools, true))
            }.execute { resp ->
                if (!resp.status.isSuccess()) throw LlmException(LlmParse.httpError(resp.status.value, resp.bodyAsText()))
                val ch = resp.bodyAsChannel()
                val acc = StreamAcc()
                while (true) {
                    val line = ch.readUTF8Line() ?: break
                    val d = acc.feed(line)
                    if (!d.isNullOrEmpty()) onDelta(d)
                    if (acc.done) break
                }
                acc.result()
            }
        } catch (e: LlmException) {
            throw e
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LlmException("Network error: " + (e.message ?: e.javaClass.simpleName))
        }
    }

    suspend fun chat(c: LlmConfig, messages: JsonArray, tools: JsonArray?, onDelta: (String) -> Unit): ParsedReply {
        var emitted = false
        try {
            return stream(c, messages, tools) {
                emitted = true
                onDelta(it)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (emitted) throw e
        }
        val r = complete(c, messages, tools)
        r.content?.let { if (it.isNotEmpty()) onDelta(it) }
        return r
    }
}
