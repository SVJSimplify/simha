package com.simha.app.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import com.simha.app.BuildConfig
import com.simha.app.LocaleCtx
import com.simha.app.R
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentLength
import io.ktor.http.isSuccess
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.IOException

data class UpdateInfo(val versionCode: Int, val versionName: String, val apkUrl: String, val notes: String)

class UpdateException(message: String) : Exception(message)

object Updater {
    const val RELEASES_URL = "https://api.github.com/repos/SVJSimplify/simha/releases?per_page=5"
    const val ACTION_RESULT = "com.simha.app.update.INSTALL_RESULT"
    private val TAG = Regex("^build-(\\d+)$")

    private val http by lazy {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                requestTimeoutMillis = 600_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 60_000
            }
        }
    }

    suspend fun check(base: Context): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        val ctx = LocaleCtx.wrapped(base)
        try {
            val resp = http.get(RELEASES_URL) {
                header("Accept", "application/vnd.github+json")
                header("X-GitHub-Api-Version", "2022-11-28")
            }
            val st = resp.status
            if (st == HttpStatusCode.Forbidden || st == HttpStatusCode.TooManyRequests) {
                val rem = resp.headers["X-RateLimit-Remaining"]
                throw UpdateException(
                    if (st == HttpStatusCode.TooManyRequests || rem == "0") ctx.getString(R.string.upd_err_rate)
                    else ctx.getString(R.string.upd_err_http, st.value)
                )
            }
            if (!st.isSuccess()) throw UpdateException(ctx.getString(R.string.upd_err_http, st.value))
            val arr = Json.parseToJsonElement(resp.bodyAsText()) as? JsonArray
                ?: throw UpdateException(ctx.getString(R.string.upd_err_parse))
            Result.success(pick(arr))
        } catch (e: UpdateException) {
            Result.failure(e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(UpdateException(ctx.getString(R.string.upd_err_net, e.message ?: "")))
        } catch (e: Exception) {
            Result.failure(UpdateException(ctx.getString(R.string.upd_err_parse)))
        }
    }

    internal fun pick(arr: JsonArray, current: Int = BuildConfig.VERSION_CODE): UpdateInfo? {
        var best: UpdateInfo? = null
        for (el in arr) {
            val o = el as? JsonObject ?: continue
            val tag = o["tag_name"]?.jsonPrimitive?.contentOrNull ?: continue
            val n = TAG.matchEntire(tag)?.groupValues?.get(1)?.toIntOrNull() ?: continue
            val asset = o["assets"]?.jsonArray?.map { it.jsonObject }
                ?.firstOrNull { it["name"]?.jsonPrimitive?.contentOrNull?.endsWith(".apk", true) == true }
                ?: continue
            val url = asset["browser_download_url"]?.jsonPrimitive?.contentOrNull ?: continue
            if (best == null || n > best.versionCode) {
                best = UpdateInfo(n, "0.1.$n", url, o["body"]?.jsonPrimitive?.contentOrNull.orEmpty().trim())
            }
        }
        return best?.takeIf { it.versionCode > current }
    }

    suspend fun downloadAndInstall(base: Context, info: UpdateInfo, onProgress: (Float) -> Unit): Result<Unit> =
        withContext(Dispatchers.IO) {
            val ctx = LocaleCtx.wrapped(base)
            try {
                if (!ctx.packageManager.canRequestPackageInstalls()) {
                    ctx.startActivity(
                        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    throw UpdateException(ctx.getString(R.string.upd_err_perm))
                }
                val dir = File(ctx.cacheDir, "updates").apply { mkdirs() }
                dir.listFiles()?.forEach { it.delete() }
                val file = File(dir, "simha-build-${info.versionCode}.apk")
                http.prepareGet(info.apkUrl).execute { r ->
                    if (!r.status.isSuccess()) throw UpdateException(ctx.getString(R.string.upd_err_http, r.status.value))
                    val total = r.contentLength() ?: -1L
                    val ch = r.bodyAsChannel()
                    val buf = ByteArray(64 * 1024)
                    var done = 0L
                    file.outputStream().use { out ->
                        while (true) {
                            val n = ch.readAvailable(buf, 0, buf.size)
                            if (n < 0) break
                            if (n == 0) continue
                            out.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress((done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                if (file.length() <= 0L) throw UpdateException(ctx.getString(R.string.upd_err_empty))
                onProgress(1f)
                install(ctx, file)
                Result.success(Unit)
            } catch (e: UpdateException) {
                Result.failure(e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                Result.failure(UpdateException(ctx.getString(R.string.upd_err_net, e.message ?: "")))
            } catch (e: Exception) {
                Result.failure(UpdateException(ctx.getString(R.string.upd_err_install, e.message ?: "")))
            }
        }

    private fun install(ctx: Context, file: File) {
        val pi = ctx.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setSize(file.length())
        }
        val id = pi.createSession(params)
        pi.openSession(id).use { s ->
            file.inputStream().use { input ->
                s.openWrite("simha.apk", 0, file.length()).use { out ->
                    input.copyTo(out)
                    s.fsync(out)
                }
            }
            val intent = Intent(ACTION_RESULT).setPackage(ctx.packageName)
            val pending = PendingIntent.getBroadcast(
                ctx, id, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            s.commit(pending.intentSender)
        }
    }
}
