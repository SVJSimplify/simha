package com.simha.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simha.app.MainViewModel
import com.simha.app.R
import com.simha.app.TodayItem
import com.simha.app.data.DoseLog
import com.simha.app.ui.meds.doseStatusText
import com.simha.app.ui.meds.showMillis
import com.simha.app.ui.meds.todayDoses
import com.simha.app.ui.theme.LocalSimha
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HealthScreen(
    vm: MainViewModel,
    onMeds: () -> Unit,
    onProfile: () -> Unit,
    onMedicalId: () -> Unit,
    onKnows: () -> Unit,
    onUnwell: () -> Unit,
    onHelp: () -> Unit,
) {
    val c = LocalSimha.current
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }
    val contacts by vm.contacts.collectAsState()
    val meds by vm.medicines.collectAsState()
    val doses by vm.doses.collectAsState()
    val zone = ZoneId.systemDefault()
    val due = remember(meds, doses, now / 60_000L) { todayDoses(meds, doses, now, zone) }
    val dayStart = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()
    val medName = { id: String? -> meds.firstOrNull { it.id == id }?.name ?: "" }
    val todayState = vm.today
    val tl = remember(todayState, doses) {
        val items = mutableListOf<Pair<Long, Any>>()
        todayState.items.forEach { items += it.at to it }
        doses.filter { it.scheduledAt >= dayStart }.forEach { items += (if (it.createdAt > 0) it.createdAt else it.scheduledAt) to it }
        items.sortedByDescending { it.first }.take(8)
    }

    TabFrame(bottom = { EmergencyButton(stringResource(R.string.m_get_help), onHelp) }) {
        AnimatedEntrance(0) { Text(stringResource(R.string.r4c_tab_health), style = MaterialTheme.typography.headlineSmall) }
        AnimatedEntrance(1, Modifier.fillMaxWidth()) {
            if (due.isEmpty()) {
                NavRow(stringResource(R.string.r3_meds_title), onMeds, hint = stringResource(R.string.r3_meds_none_today))
            } else {
                Card {
                    due.forEach { d ->
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Column(Modifier.weight(1f)) {
                                Body(d.medicine.name + if (d.medicine.dose.isBlank()) "" else " " + d.medicine.dose)
                                Caption(showMillis(d.at))
                            }
                            if (d.status == null) {
                                Chip(stringResource(R.string.r3_dose_taken), false, { vm.logDose(d.medicine.id, d.at, "taken") })
                            } else {
                                Body(doseStatusText(d.status), color = c.success)
                            }
                        }
                    }
                    SecondaryButton(stringResource(R.string.r3_meds_title), onMeds)
                }
            }
        }
        AnimatedEntrance(2, Modifier.fillMaxWidth()) { NavRow(stringResource(R.string.profile_title), onProfile) }
        AnimatedEntrance(3, Modifier.fillMaxWidth()) { NavRow(stringResource(R.string.r3_mid_title), onMedicalId) }
        AnimatedEntrance(3, Modifier.fillMaxWidth()) { NavRow(stringResource(R.string.cf_knows_title), onKnows) }
        AnimatedEntrance(4, Modifier.fillMaxWidth()) {
            NavRow(stringResource(R.string.r3_unwell), onUnwell, icon = R.drawable.ic_heart, tint = c.emergency)
        }
        AnimatedEntrance(5, Modifier.fillMaxWidth()) {
            Card {
                Text(stringResource(R.string.r3_timeline), style = MaterialTheme.typography.titleMedium)
                if (tl.isEmpty()) Caption(stringResource(R.string.r3_tl_empty))
                tl.forEach { (at, any) ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Caption(showMillis(at), Modifier.widthIn(min = 72.dp))
                        Body(
                            when (any) {
                                is TodayItem -> todayText(any, contacts.firstOrNull { it.phone == any.arg }?.name ?: any.arg)
                                is DoseLog -> doseStatusText(any.status) + " " + medName(any.medicineId)
                                else -> ""
                            },
                            Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}
