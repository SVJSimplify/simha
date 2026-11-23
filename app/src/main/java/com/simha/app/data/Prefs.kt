package com.simha.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.simhaStore: DataStore<Preferences> by preferencesDataStore("simha")

data class Snap(
    val setup: Boolean = false,
    val role: String? = null,
    val name: String = "",
    val lang: String = "en",
    val callFamily: Boolean = false,
    val callHospital: Boolean = false,
    val record: Boolean = false,
    val inactivity: Int = 180,
    val wait: Int = 5,
    val lastActivity: Long = 0,
    val lastPrompt: Long = 0,
    val lastCheckinOk: Long = 0,
    val escalatedFor: Long = 0,
    val lastHeartbeat: Long = 0,
    val night: Boolean = false,
    val personId: String? = null,
    val personName: String = "",
    val running: Boolean = true,
    val perms: String = "",
)

object K {
    val SETUP = booleanPreferencesKey("setup")
    val ROLE = stringPreferencesKey("role")
    val NAME = stringPreferencesKey("name")
    val LANG = stringPreferencesKey("lang")
    val CALL_FAMILY = booleanPreferencesKey("call_family")
    val CALL_HOSPITAL = booleanPreferencesKey("call_hospital")
    val RECORD = booleanPreferencesKey("record")
    val INACTIVITY = intPreferencesKey("inactivity")
    val WAIT = intPreferencesKey("wait")
    val LAST_ACTIVITY = longPreferencesKey("last_activity")
    val LAST_PROMPT = longPreferencesKey("last_prompt")
    val LAST_CHECKIN_OK = longPreferencesKey("last_checkin_ok")
    val ESCALATED_FOR = longPreferencesKey("escalated_for")
    val LAST_HEARTBEAT = longPreferencesKey("last_heartbeat")
    val NIGHT = booleanPreferencesKey("night")
    val PERSON_ID = stringPreferencesKey("person_id")
    val PERSON_NAME = stringPreferencesKey("person_name")
    val RUNNING = booleanPreferencesKey("running")
    val PERMS = stringPreferencesKey("perms")
}

class Prefs(private val c: Context) {
    val flow: Flow<Snap> = c.simhaStore.data.map { p ->
        Snap(
            setup = p[K.SETUP] ?: false,
            role = p[K.ROLE],
            name = p[K.NAME] ?: "",
            lang = p[K.LANG] ?: java.util.Locale.getDefault().language.takeIf { it in setOf("en", "hi", "te", "kn", "ta") } ?: "en",
            callFamily = p[K.CALL_FAMILY] ?: false,
            callHospital = p[K.CALL_HOSPITAL] ?: false,
            record = p[K.RECORD] ?: false,
            inactivity = p[K.INACTIVITY] ?: 180,
            wait = p[K.WAIT] ?: 5,
            lastActivity = p[K.LAST_ACTIVITY] ?: 0L,
            lastPrompt = p[K.LAST_PROMPT] ?: 0L,
            lastCheckinOk = p[K.LAST_CHECKIN_OK] ?: 0L,
            escalatedFor = p[K.ESCALATED_FOR] ?: 0L,
            lastHeartbeat = p[K.LAST_HEARTBEAT] ?: 0L,
            night = p[K.NIGHT] ?: false,
            personId = p[K.PERSON_ID],
            personName = p[K.PERSON_NAME] ?: "",
            running = p[K.RUNNING] ?: true,
            perms = p[K.PERMS] ?: "",
        )
    }

    suspend fun snap(): Snap = flow.first()

    suspend fun edit(block: MutablePreferences.() -> Unit) {
        c.simhaStore.edit { it.block() }
    }
}

object OBK {
    val STEP = intPreferencesKey("ob_step")
    val HELPER = booleanPreferencesKey("ob_helper")
    val ANSWERED = stringPreferencesKey("ob_answered")
    val HOSPITAL = stringPreferencesKey("hospital_name")
}
