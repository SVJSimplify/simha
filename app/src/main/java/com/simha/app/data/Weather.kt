package com.simha.app.data

import android.content.Context
import android.util.Log
import com.simha.app.Actions
import com.simha.app.wake.WakeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale

data class WeatherReport(val day: String, val maxC: Double?, val aqi: Int?)

object Weather {
    const val HEAT_C = 40.0
    const val AQI_BAD = 150
    private const val RETRY_MS = 60L * 60_000L

    fun parseMax(json: String): Double? = try {
        Json.parseToJsonElement(json).jsonObject["daily"]?.jsonObject?.get("temperature_2m_max")?.jsonArray?.firstOrNull()?.jsonPrimitive?.doubleOrNull
    } catch (e: Exception) {
        null
    }

    fun parseAqi(json: String): Int? = try {
        Json.parseToJsonElement(json).jsonObject["current"]?.jsonObject?.get("us_aqi")?.jsonPrimitive?.doubleOrNull?.toInt()
    } catch (e: Exception) {
        null
    }

    fun isHeat(r: WeatherReport): Boolean = (r.maxC ?: 0.0) >= HEAT_C

    fun isBadAir(r: WeatherReport): Boolean = (r.aqi ?: 0) >= AQI_BAD

    fun noteFor(r: WeatherReport): String? {
        val parts = ArrayList<String>()
        if (isHeat(r)) parts.add("Heat alert: the forecast high today is ${r.maxC?.toInt()} C. Gently remind the user to drink water, stay indoors in the afternoon and keep cool.")
        if (isBadAir(r)) parts.add("Air quality is poor today (AQI ${r.aqi}). Suggest staying indoors with windows closed.")
        return parts.joinToString(" ").ifBlank { null }
    }

    private suspend fun get(url: String): Result<String> = withContext(Dispatchers.IO) {
        var c: HttpURLConnection? = null
        try {
            c = URL(url).openConnection() as HttpURLConnection
            c.connectTimeout = 5000
            c.readTimeout = 5000
            if (c.responseCode !in 200..299) {
                Result.failure(IOException("HTTP " + c.responseCode))
            } else {
                Result.success(c.inputStream.bufferedReader().use { it.readText() })
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            c?.disconnect()
        }
    }

    private fun cached(ctx: Context, day: String): WeatherReport? {
        if (WakeStore.str(ctx, "w_day") != day) return null
        val max = WakeStore.str(ctx, "w_max")?.toDoubleOrNull()
        val aqi = WakeStore.str(ctx, "w_aqi")?.toIntOrNull()
        return if (max == null && aqi == null) null else WeatherReport(day, max, aqi)
    }

    suspend fun today(ctx: Context, allowFetch: Boolean): Result<WeatherReport> {
        val day = LocalDate.now().toString()
        cached(ctx, day)?.let { return Result.success(it) }
        if (!allowFetch) return Result.failure(IllegalStateException("Weather not loaded yet"))
        val now = System.currentTimeMillis()
        if (now - WakeStore.long(ctx, "w_try") < RETRY_MS) return Result.failure(IllegalStateException("Weather retry later"))
        WakeStore.put(ctx, "w_try" to now)
        val loc = Actions.lastLocation(ctx) ?: return Result.failure(IllegalStateException("No location available"))
        val lat = String.format(Locale.US, "%.2f", loc.latitude)
        val lon = String.format(Locale.US, "%.2f", loc.longitude)
        val (t, a) = coroutineScope {
            val t = async { get("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&daily=temperature_2m_max&timezone=auto&forecast_days=1") }
            val a = async { get("https://air-quality-api.open-meteo.com/v1/air-quality?latitude=$lat&longitude=$lon&current=us_aqi") }
            t.await() to a.await()
        }
        val max = t.getOrNull()?.let { parseMax(it) }
        val aqi = a.getOrNull()?.let { parseAqi(it) }
        if (max == null && aqi == null) {
            val e = t.exceptionOrNull() ?: a.exceptionOrNull() ?: IOException("Weather response not understood")
            Log.w("Simha", "weather failed: " + e.message)
            return Result.failure(e)
        }
        WakeStore.put(ctx, "w_day" to day, "w_max" to max?.toString(), "w_aqi" to aqi?.toString())
        return Result.success(WeatherReport(day, max, aqi))
    }

    suspend fun note(ctx: Context, allowFetch: Boolean): String? {
        val r = withTimeoutOrNull(8000L) { today(ctx, allowFetch) } ?: return null
        return r.getOrNull()?.let { noteFor(it) }
    }

    suspend fun isHeatDay(ctx: Context): Boolean = today(ctx, false).getOrNull()?.let { isHeat(it) } ?: false

    suspend fun refreshIfDue(ctx: Context) {
        if (LocalTime.now().hour < 5) return
        today(ctx, true)
    }
}
