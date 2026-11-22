package com.simha.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.simha.app.MainViewModel
import com.simha.app.R
import com.simha.app.data.AuditRow
import com.simha.app.data.PersonView
import com.simha.app.data.Snap
import com.simha.app.logic.TimeUtil
import kotlinx.coroutines.launch

@Composable
private fun eventLabel(type: String): String = stringResource(
    when (type) {
        "heartbeat" -> R.string.ev_heartbeat
        "checkin" -> R.string.ev_checkin
        "checkin_ok" -> R.string.ev_checkin_ok
        "checkin_missed" -> R.string.ev_checkin_missed
        "alert" -> R.string.ev_alert
        else -> R.string.ev_help
    }
)

private fun shortTime(iso: String): String = iso.take(16).replace('T', ' ')

@Composable
fun CaregiverSetupScreen(vm: MainViewModel, s: Snap, onDone: () -> Unit) {
    val scope = rememberCoroutineScope()
    var code by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val title = stringResource(R.string.cg_setup_title)
    ScreenFrame(title, title, null) {
        Body(stringResource(R.string.cg_setup_body))
        LabeledField(stringResource(R.string.cg_code_label), code, { code = it.uppercase() }, singleLine = true)
        BigButton(stringResource(R.string.cg_join), {
            scope.launch {
                busy = true
                err = null
                vm.sl.repo.acceptInvite(code).fold(
                    { id ->
                        val name = vm.sl.repo.personView(id).getOrNull()?.name ?: ""
                        vm.bindPerson(id, name)
                        onDone()
                    },
                    { err = it.message ?: it.javaClass.simpleName },
                )
                busy = false
            }
        }, enabled = code.isNotBlank() && !busy)
        err?.let { Body(stringResource(R.string.cg_join_failed, it), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun CaregiverHomeScreen(vm: MainViewModel, s: Snap, onSwitch: () -> Unit) {
    val scope = rememberCoroutineScope()
    var view by remember { mutableStateOf<PersonView?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val reload: () -> Unit = {
        val id = s.personId
        if (id != null) {
            scope.launch {
                vm.sl.repo.personView(id).fold(
                    { view = it; err = null },
                    { err = it.message ?: it.javaClass.simpleName },
                )
            }
        }
    }
    LaunchedEffect(Unit) { reload() }
    val v = view
    val name = (v?.name ?: "").ifBlank { s.personName }
    val title = stringResource(R.string.cg_home_title, name)
    val lastOk = v?.events?.firstOrNull { it.type == "checkin_ok" }?.let { TimeUtil.isoMillis(it.createdAt) }
    val now = System.currentTimeMillis()
    ScreenFrame(title, title, null) {
        err?.let { Body(stringResource(R.string.sync_error, it), color = MaterialTheme.colorScheme.error) }
        Card { Body(stringResource(R.string.cg_last, agoText(TimeUtil.ago(now, lastOk)))) }
        if (v != null) {
            val alerts = v.events.filter { it.type == "alert" || it.type == "checkin_missed" || it.type == "help" }
            Card {
                Body(stringResource(R.string.cg_alerts))
                if (alerts.isEmpty()) Caption(stringResource(R.string.cg_empty))
                alerts.take(10).forEach { Caption(shortTime(it.createdAt) + "  " + eventLabel(it.type)) }
            }
            Card {
                Body(stringResource(R.string.cg_calls))
                if (v.calls.isEmpty()) Caption(stringResource(R.string.cg_empty))
                v.calls.take(10).forEach { Caption(shortTime(it.createdAt) + "  " + it.number + "  " + it.reason + "  " + it.outcome) }
            }
            Card {
                Body(stringResource(R.string.cg_recent))
                if (v.events.isEmpty()) Caption(stringResource(R.string.cg_empty))
                v.events.take(10).forEach { Caption(shortTime(it.createdAt) + "  " + eventLabel(it.type)) }
            }
            Card {
                if (v.canViewRecordings) {
                    Body(stringResource(R.string.cg_recordings, v.recordings.size))
                    v.recordings.forEach { Caption(shortTime(it.createdAt) + "  " + it.storageRef.substringAfterLast('/')) }
                } else {
                    Body(stringResource(R.string.cg_no_recordings))
                }
            }
        }
        BigButton(stringResource(R.string.cg_refresh), reload)
        BigButton(stringResource(R.string.cg_switch), onSwitch, kind = Kind.Secondary)
    }
}

@Composable
fun AuditScreen(vm: MainViewModel, onBack: () -> Unit) {
    var rows by remember { mutableStateOf<List<AuditRow>?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val reload: () -> Unit = {
        scope.launch {
            vm.sl.repo.auditLog().fold(
                { rows = it; err = null },
                { err = it.message ?: it.javaClass.simpleName },
            )
        }
    }
    LaunchedEffect(Unit) { reload() }
    val title = stringResource(R.string.audit_title)
    ScreenFrame(title, title, onBack) {
        err?.let { Body(stringResource(R.string.sync_error, it), color = MaterialTheme.colorScheme.error) }
        val r = rows
        if (r != null && r.isEmpty()) Body(stringResource(R.string.audit_empty))
        r?.forEach { Card { Caption(shortTime(it.createdAt)); Body(it.action) } }
        BigButton(stringResource(R.string.cg_refresh), reload)
    }
}
