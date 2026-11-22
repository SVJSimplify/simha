package com.simha.app

import android.app.Application
import android.content.Intent
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simha.app.data.ContactEntity
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
            sl.prefs.edit { this[K.LANG] = code }
            if (sl.prefs.snap().setup) sl.repo.syncUser()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code))
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
}
