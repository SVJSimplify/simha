package com.simha.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.simha.app.MainViewModel
import com.simha.app.R
import com.simha.app.data.Snap
import com.simha.app.logic.TimeUtil
import kotlinx.coroutines.delay

@Composable
fun RoleScreen(onSelf: () -> Unit, onCaregiver: () -> Unit) {
    val title = stringResource(R.string.role_title)
    ScreenFrame(title, title, null) {
        BigButton(stringResource(R.string.role_self), onSelf)
        BigButton(stringResource(R.string.role_caregiver), onCaregiver)
    }
}

@Composable
fun HomeScreen(
    vm: MainViewModel,
    s: Snap,
    onTalk: () -> Unit,
    onFamily: () -> Unit,
    onDoctor: () -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
) {
    val l = LocalLayout.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }
    val err by vm.syncError.collectAsState()
    val status = stringResource(R.string.home_status, agoText(TimeUtil.ago(now, s.lastCheckinOk.takeIf { it > 0 })))
    ScreenFrame(stringResource(R.string.app_name), status, null) {
        Body(status)
        if (!s.running) {
            Card {
                Body(stringResource(R.string.stopped_banner))
                BigButton(stringResource(R.string.set_start), { vm.startSimha() })
            }
        }
        err?.let { Card { Body(stringResource(R.string.sync_error, it)) } }
        BigButton(stringResource(R.string.talk), onTalk, icon = R.drawable.ic_mic)
        val actions: List<@Composable (Modifier) -> Unit> = listOf(
            { m: Modifier -> BigButton(stringResource(R.string.family), onFamily, m) },
            { m: Modifier -> BigButton(stringResource(R.string.doctor), onDoctor, m) },
            { m: Modifier -> BigButton(stringResource(R.string.help), onHelp, m, Kind.Emergency) },
        )
        Grid(l.columns, actions)
        SwitchRow(stringResource(R.string.night), s.night) { vm.setNight(it) }
        val more: List<@Composable (Modifier) -> Unit> = listOf(
            { m: Modifier -> BigButton(stringResource(R.string.profile_title), onProfile, m, Kind.Secondary) },
            { m: Modifier -> BigButton(stringResource(R.string.set_title), onSettings, m, Kind.Secondary) },
        )
        Grid(l.columns.coerceAtMost(2), more)
    }
}
