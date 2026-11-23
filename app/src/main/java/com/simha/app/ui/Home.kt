package com.simha.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.simha.app.MainViewModel
import com.simha.app.R
import com.simha.app.data.CallResult
import com.simha.app.data.Repo
import com.simha.app.data.Snap
import com.simha.app.logic.TimeUtil
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.ui.theme.SimhaRadius
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun RoleScreen(onSelf: () -> Unit, onCaregiver: () -> Unit) {
    val title = stringResource(R.string.role_title)
    ScreenFrame(title, title, null) {
        PrimaryButton(stringResource(R.string.role_self), onSelf)
        SecondaryButton(stringResource(R.string.role_caregiver), onCaregiver)
    }
}

@Composable
private fun HeroCard(title: String, subtitle: String, onClick: () -> Unit) {
    val c = LocalSimha.current
    Column(
        Modifier
            .fillMaxWidth()
            .pressClick(onClick)
            .clip(RoundedCornerShape(SimhaRadius))
            .background(c.primary)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(72.dp).background(c.onPrimary.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_mic), null, Modifier.size(36.dp), tint = c.onPrimary)
        }
        Text(title, style = MaterialTheme.typography.labelLarge, color = c.onPrimary, textAlign = TextAlign.Center)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = c.onPrimary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ActionTile(icon: Int, text: String, onClick: () -> Unit, modifier: Modifier) {
    val c = LocalSimha.current
    val l = LocalLayout.current
    val shape = RoundedCornerShape(SimhaRadius)
    Column(
        modifier
            .heightIn(min = l.primaryH)
            .pressClick(onClick)
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(icon), null, Modifier.size(28.dp), tint = c.primary)
        Text(text, style = MaterialTheme.typography.labelLarge, color = c.text, textAlign = TextAlign.Center)
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
    val c = LocalSimha.current
    val scope = rememberCoroutineScope()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }
    val err by vm.syncError.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val prof by vm.profile.collectAsState()
    var failed by remember { mutableStateOf(false) }
    val status = stringResource(R.string.home_status, agoText(TimeUtil.ago(now, s.lastCheckinOk.takeIf { it > 0 })))
    val greet = if (s.name.isBlank()) stringResource(R.string.m_greet_plain) else stringResource(R.string.m_greet, s.name)
    val shape = RoundedCornerShape(SimhaRadius)

    val callFamily: () -> Unit = {
        val f = contacts.firstOrNull { it.role == "family" } ?: contacts.firstOrNull { it.role in Repo.FAMILY_ROLES }
        if (f == null) onFamily() else scope.launch {
            failed = vm.sl.repo.placeCall(f.phone, f.approved, "user_button") == CallResult.Failed
        }
    }
    val callDoctor: () -> Unit = {
        val d = contacts.firstOrNull { it.role == "doctor" }
        val phone = d?.phone ?: prof.doctorPhone.takeIf { it.isNotBlank() }
        if (phone == null) onDoctor() else scope.launch {
            failed = vm.sl.repo.placeCall(phone, d?.approved == true, "user_button") == CallResult.Failed
        }
    }

    CompositionLocalProvider(LocalContentColor provides c.text) {
        Box(
            Modifier
                .fillMaxSize()
                .background(c.page)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = SimhaRadius, bottomEnd = SimhaRadius))
                            .background(c.fill)
                            .padding(start = l.pad, end = l.pad, top = 28.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(greet, style = MaterialTheme.typography.titleLarge)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(shape)
                                .background(c.surface)
                                .border(1.dp, c.border, shape)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(Modifier.size(12.dp).background(if (s.running) c.success else c.text2, CircleShape))
                            Text(status, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        }
                    }
                    Column(
                        Modifier.fillMaxWidth().padding(start = l.pad, end = l.pad, top = 16.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (!s.running) {
                            Card {
                                Body(stringResource(R.string.stopped_banner))
                                PrimaryButton(stringResource(R.string.set_start), { vm.startSimha() })
                            }
                        }
                        err?.let { Card { Body(stringResource(R.string.sync_error, it)) } }
                        HeroCard(stringResource(R.string.talk), stringResource(R.string.m_talk_sub), onTalk)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ActionTile(R.drawable.ic_users, stringResource(R.string.m_call_family), callFamily, Modifier.weight(1f))
                            ActionTile(R.drawable.ic_phone, stringResource(R.string.m_call_doctor), callDoctor, Modifier.weight(1f))
                        }
                        if (failed) Body(stringResource(R.string.call_failed), color = c.emergency)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextLink(stringResource(R.string.m_link_profile), onProfile)
                            TextLink(stringResource(R.string.m_link_settings), onSettings)
                        }
                    }
                }
                Box(Modifier.fillMaxWidth().padding(horizontal = l.pad, vertical = 12.dp)) {
                    EmergencyButton(stringResource(R.string.m_get_help), onHelp)
                }
            }
        }
    }
}
