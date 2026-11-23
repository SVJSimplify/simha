package com.simha.app.ui.home

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object UiKeys {
    val HEY = booleanPreferencesKey("hey_simha")
    val NIGHT_WATCH = booleanPreferencesKey("night_watch")
    val FALL = booleanPreferencesKey("fall_detection")
    val SAFE = booleanPreferencesKey("safe_area_enabled")
    val MORNING = stringPreferencesKey("greet_morning")
    val EVENING = stringPreferencesKey("greet_evening")
    val BLOOD = stringPreferencesKey("blood_group")
    val ID_LOCK = booleanPreferencesKey("medical_id_lock")
}

data class UiSettings(
    val hey: Boolean = false,
    val nightWatch: Boolean = false,
    val fall: Boolean = false,
    val safe: Boolean = false,
    val morning: String = "08:00",
    val evening: String = "20:00",
    val blood: String = "",
    val idLock: Boolean = false,
)

fun Preferences.toUi() = UiSettings(
    hey = this[UiKeys.HEY] ?: false,
    nightWatch = this[UiKeys.NIGHT_WATCH] ?: false,
    fall = this[UiKeys.FALL] ?: false,
    safe = this[UiKeys.SAFE] ?: false,
    morning = this[UiKeys.MORNING] ?: "08:00",
    evening = this[UiKeys.EVENING] ?: "20:00",
    blood = this[UiKeys.BLOOD] ?: "",
    idLock = this[UiKeys.ID_LOCK] ?: false,
)
