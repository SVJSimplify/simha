package com.simha.app.data

import com.simha.app.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.Storage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class Remote {
    val sb = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_KEY) {
        install(Auth)
        install(Postgrest)
        install(Storage)
    }

    suspend fun uid(): String {
        sb.auth.awaitInitialization()
        sb.auth.currentUserOrNull()?.id?.let { return it }
        sb.auth.signInAnonymously()
        return sb.auth.currentUserOrNull()?.id ?: throw IllegalStateException("Sign-in did not return a user")
    }

    private fun withUid(o: JsonObject, tbl: String, uid: String): JsonObject {
        val key = if (tbl == "users") "id" else "user_id"
        return JsonObject(o + (key to JsonPrimitive(uid)))
    }

    private val CONFLICT = mapOf("diary" to "user_id,day", "daily_signal" to "user_id,day", "weekly_signal" to "user_id,week", "facts" to "user_id,k")

    suspend fun apply(op: OutboxEntity, uid: String) {
        val o = Json.parseToJsonElement(op.payload).jsonObject
        when (op.kind) {
            "insert" -> sb.from(op.tbl).insert(withUid(o, op.tbl, uid))
            "upsert" -> {
                val c = CONFLICT[op.tbl]
                if (c != null) sb.from(op.tbl).upsert(withUid(o, op.tbl, uid)) { onConflict = c } else sb.from(op.tbl).upsert(withUid(o, op.tbl, uid))
            }
            "delete" -> if (op.tbl == "facts") {
                val k = o.getValue("k").jsonPrimitive.content
                sb.from(op.tbl).delete { filter { eq("user_id", uid); eq("k", k) } }
            } else {
                val id = o.getValue("id").jsonPrimitive.content
                sb.from(op.tbl).delete { filter { eq("id", id) } }
            }
            else -> throw IllegalStateException("Unknown outbox kind " + op.kind)
        }
    }
}
