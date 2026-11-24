package com.simha.app

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simha.app.data.ContactEntity
import com.simha.app.data.DoseLog
import com.simha.app.data.Medicine
import com.simha.app.data.simhaStore
import com.simha.app.ui.home.UiKeys
import com.simha.app.ui.home.UiSettings
import com.simha.app.ui.home.toUi
import com.simha.app.monitor.SafeArea
import com.simha.app.wake.Wake
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId
import com.simha.app.data.K
import com.simha.app.data.ProfileEntity
import com.simha.app.data.Snap
import com.simha.app.data.jo
import com.simha.app.monitor.ActivityService
import com.simha.app.monitor.Work
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    val sl = (app as SimhaApp).sl
    val snap: StateFlow<Snap?> = sl.prefs.flow.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val contacts: StateFlow<List<ContactEntity>> = sl.repo.contacts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val profile: StateFlow<ProfileEntity> = sl.repo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileEntity())
    val syncError: StateFlow<String?> = sl.repo.syncError
    val route = MutableStateFlow<String?>(null)
    val resume = MutableStateFlow(0)
    val medicines: StateFlow<List<Medicine>> = sl.guardian.medicines().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val doses: StateFlow<List<DoseLog>> = sl.guardian.doses(
        LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    ).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val ui: StateFlow<UiSettings> = app.simhaStore.data.map { it.toUi() }.stateIn(viewModelScope, SharingStarted.Eagerly, UiSettings())
    var today by mutableStateOf(TodayState())

    var obStep by mutableIntStateOf(0)
    var obPerm by mutableIntStateOf(0)
    var obApproval by mutableIntStateOf(0)
    var obName by mutableStateOf("")

    fun handleIntent(i: Intent?) {
        val action = i?.action ?: return
        i.action = null
        when (action) {
            MainActivity.ACTION_CHECKIN -> route.value = "talk/checkin"
            MainActivity.ACTION_ESCALATE -> {
                val number = i.getStringExtra(MainActivity.EXTRA_NUMBER) ?: return
                viewModelScope.launch {
                    val c = sl.repo.contactsNow().firstOrNull { it.phone == number }
                    val r = sl.repo.placeCall(number, c?.approved == true, "no_reply_checkin")
                    if (r == com.simha.app.data.CallResult.Failed) route.value = "contacts/family"
                }
            }
        }
    }

    fun onResume() {
        resume.value = resume.value + 1
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            val p = sl.prefs.snap()
            if (p.role == "self" && p.setup) {
                if (p.running) ActivityService.start(ctx)
                val now = Perms.snapshot(ctx)
                if (p.perms.isNotEmpty() && p.perms != now) sl.repo.audit("permissions_changed", jo("before" to p.perms, "after" to now))
                if (p.perms != now) sl.prefs.edit { this[K.PERMS] = now }
            }
        }
    }

    fun setRole(role: String) {
        viewModelScope.launch { sl.prefs.edit { this[K.ROLE] = role } }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            LocaleCtx.save(getApplication(), code)
            sl.prefs.edit { this[K.LANG] = code }
            if (sl.prefs.snap().setup) sl.repo.syncUser()
        }
    }

    fun setNight(on: Boolean) {
        viewModelScope.launch { sl.prefs.edit { this[K.NIGHT] = on } }
    }

    fun saveName(name: String) {
        viewModelScope.launch { sl.prefs.edit { this[K.NAME] = name.trim() } }
    }

    fun finishOnboarding() {
        viewModelScope.launch {
            sl.prefs.edit {
                this[K.NAME] = obName.trim()
                this[K.SETUP] = true
                this[K.ROLE] = "self"
                this[K.LAST_ACTIVITY] = System.currentTimeMillis()
            }
            sl.repo.syncUser()
            sl.repo.audit("onboarding_done")
            Work.schedule(getApplication())
            ActivityService.start(getApplication())
        }
    }

    fun setApproval(kind: String, on: Boolean) {
        viewModelScope.launch { sl.repo.setApproval(kind, on) }
    }

    fun saveProfile(p: ProfileEntity) {
        viewModelScope.launch { sl.repo.saveProfile(p) }
    }

    fun addContact(name: String, phone: String, role: String) {
        viewModelScope.launch { sl.repo.addContact(name, phone, role) }
    }

    fun removeContact(id: String) {
        viewModelScope.launch { sl.repo.removeContact(id) }
    }

    fun saveRules(inactivity: Int, wait: Int) {
        viewModelScope.launch { sl.repo.saveRules(inactivity, wait) }
    }

    fun checkinOk() {
        viewModelScope.launch { sl.repo.checkinOk() }
    }

    fun stopSimha() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            sl.prefs.edit { this[K.RUNNING] = false }
            ActivityService.stop(ctx)
            Work.cancel(ctx)
            sl.repo.audit("simha_stopped")
        }
    }

    fun startSimha() {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            sl.prefs.edit { this[K.RUNNING] = true }
            Work.schedule(ctx)
            ActivityService.start(ctx)
            sl.repo.audit("simha_started")
        }
    }

    fun bindPerson(id: String, name: String) {
        viewModelScope.launch {
            sl.prefs.edit {
                this[K.PERSON_ID] = id
                this[K.PERSON_NAME] = name
                this[K.ROLE] = "caregiver"
            }
        }
    }

    fun clearPerson() {
        viewModelScope.launch {
            sl.prefs.edit {
                remove(K.PERSON_ID)
                remove(K.PERSON_NAME)
            }
        }
    }

    private fun edit(wake: Boolean, block: MutablePreferences.() -> Unit) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            ctx.simhaStore.edit { it.block() }
            if (wake) Wake.scheduleAll(ctx)
        }
    }

    fun setHey(on: Boolean) = edit(false) { this[UiKeys.HEY] = on }
    fun setNightWatch(on: Boolean) = edit(true) { this[UiKeys.NIGHT_WATCH] = on }
    fun setFall(on: Boolean) = edit(true) { this[UiKeys.FALL] = on }
    fun setSafe(on: Boolean) = edit(true) { this[UiKeys.SAFE] = on }
    fun setMorning(t: String) = edit(true) { this[UiKeys.MORNING] = t }
    fun setEvening(t: String) = edit(true) { this[UiKeys.EVENING] = t }
    fun setBlood(v: String) = edit(false) { this[UiKeys.BLOOD] = v }
    fun setIdLock(on: Boolean) = edit(false) { this[UiKeys.ID_LOCK] = on }

    suspend fun setHomeHere(): Boolean = withContext(Dispatchers.IO) {
        try {
            val ok = SafeArea.setHomeHere(getApplication())
            if (ok) sl.repo.audit("safe_area_home_set")
            ok
        } catch (e: Exception) {
            false
        }
    }

    fun saveMedicine(m: Medicine) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            try {
                sl.guardian.saveMedicine(m)
            } catch (e: IllegalArgumentException) {
                return@launch
            }
            Wake.scheduleAll(ctx)
        }
    }

    fun deleteMedicine(id: String) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            sl.guardian.deleteMedicine(id)
            Wake.scheduleAll(ctx)
        }
    }

    fun logDose(medicineId: String?, at: Long, status: String) {
        val ctx = getApplication<Application>()
        viewModelScope.launch {
            try {
                sl.guardian.logDose(medicineId, at, status)
            } catch (e: IllegalArgumentException) {
                return@launch
            }
            Wake.scheduleAll(ctx)
        }
    }

    fun refreshToday() {
        viewModelScope.launch {
            val diary = sl.guardian.diaryFor(LocalDate.now())
            val mood = diary?.sig?.mood?.toString()
            val start = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val items = mutableListOf<TodayItem>()
            val uid = try {
                sl.remote.uid()
            } catch (e: Exception) {
                null
            }
            uid?.let { sl.repo.personView(it).getOrNull() }?.let { v ->
                v.events.filter { it.type in setOf("checkin_ok", "checkin_missed", "help") }.forEach { e ->
                    val t = com.simha.app.logic.TimeUtil.isoMillis(e.createdAt)
                    if (t != null && t >= start) items += TodayItem(t, e.type, "")
                }
                v.calls.forEach { c ->
                    val t = com.simha.app.logic.TimeUtil.isoMillis(c.createdAt)
                    if (t != null && t >= start) items += TodayItem(t, "call", c.number)
                }
            }
            today = TodayState(mood, items.sortedByDescending { it.at })
        }
    }
}

data class TodayItem(val at: Long, val kind: String, val arg: String)

data class TodayState(val mood: String? = null, val items: List<TodayItem> = emptyList())
