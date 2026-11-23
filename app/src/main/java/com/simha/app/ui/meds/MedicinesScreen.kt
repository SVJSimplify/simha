package com.simha.app.ui.meds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simha.app.MainViewModel
import com.simha.app.R
import com.simha.app.data.Medicine
import com.simha.app.logic.parseTime
import com.simha.app.ui.Body
import com.simha.app.ui.Caption
import com.simha.app.ui.Card
import com.simha.app.ui.LabeledField
import com.simha.app.ui.PrimaryButton
import com.simha.app.ui.ScreenFrame
import com.simha.app.ui.SecondaryButton
import com.simha.app.ui.SectionTitle
import com.simha.app.ui.SwitchRow
import com.simha.app.ui.TextLink
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.ui.theme.SimhaRadius
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.UUID

fun showTime(s: String): String =
    parseTime(s)?.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)) ?: s

fun showMillis(ms: Long): String =
    java.time.Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(initial: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    val t = parseTime(initial) ?: LocalTime.of(8, 0)
    val st = rememberTimePickerState(t.hour, t.minute, false)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({ onPick(String.format(Locale.US, "%02d:%02d", st.hour, st.minute)) }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.m_cancel)) } },
        text = { TimePicker(st) },
    )
}

@Composable
private fun TimeTag(text: String) {
    val c = LocalSimha.current
    val shape = RoundedCornerShape(SimhaRadius)
    Text(
        showTime(text),
        style = MaterialTheme.typography.bodyLarge,
        color = c.text,
        modifier = Modifier.clip(shape).background(c.fill).border(1.dp, c.border, shape).padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun MedicineForm(initial: Medicine?, onSave: (Medicine) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var dose by remember { mutableStateOf(initial?.dose ?: "") }
    var times by remember { mutableStateOf(initial?.times ?: emptyList()) }
    var stock by remember { mutableStateOf(initial?.stock?.toString() ?: "") }
    var picking by remember { mutableStateOf(false) }
    var tried by remember { mutableStateOf(false) }
    val stockNum = stock.trim().toIntOrNull()
    val stockBad = stock.isNotBlank() && (stockNum == null || stockNum < 0)
    Card {
        LabeledField(
            stringResource(R.string.r3_m_name), name, { name = it }, required = true,
            error = if (tried && name.isBlank()) stringResource(R.string.r3_m_err_name) else null,
        )
        LabeledField(stringResource(R.string.r3_m_dose), dose, { dose = it })
        Body(stringResource(R.string.r3_m_times))
        times.forEach { t ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TimeTag(t)
                TextLink(stringResource(R.string.remove)) { times = times - t }
            }
        }
        if (tried && times.isEmpty()) Body(stringResource(R.string.r3_m_err_time), color = LocalSimha.current.emergency)
        SecondaryButton(stringResource(R.string.r3_m_add_time), { picking = true })
        LabeledField(
            stringResource(R.string.r3_m_stock), stock, { stock = it }, keyboard = KeyboardType.Number,
            error = if (stockBad) stringResource(R.string.r3_m_err_stock) else null,
        )
        PrimaryButton(stringResource(R.string.save), {
            tried = true
            if (name.isNotBlank() && times.isNotEmpty() && !stockBad) {
                onSave(
                    Medicine(
                        initial?.id ?: UUID.randomUUID().toString(), name.trim(), dose.trim(),
                        times.distinct().sorted(), stockNum, initial?.active ?: true,
                    )
                )
            }
        })
        TextLink(stringResource(R.string.m_cancel), onCancel)
    }
    if (picking) {
        TimeDialog("08:00", { t -> if (t !in times) times = times + t; picking = false }, { picking = false })
    }
}

@Composable
fun MedicinesScreen(vm: MainViewModel, onBack: () -> Unit) {
    val meds by vm.medicines.collectAsState()
    val doses by vm.doses.collectAsState()
    val now by produceState(System.currentTimeMillis()) {
        while (true) {
            delay(30_000L)
            value = System.currentTimeMillis()
        }
    }
    val due = remember(meds, doses, now / 60_000L) { todayDoses(meds, doses, now, ZoneId.systemDefault()) }
    var editing by remember { mutableStateOf<Medicine?>(null) }
    var adding by remember { mutableStateOf(false) }
    val title = stringResource(R.string.r3_meds_title)
    ScreenFrame(title, title, onBack) {
        SectionTitle(stringResource(R.string.r3_meds_today))
        if (due.isEmpty()) Body(stringResource(R.string.r3_meds_none_today))
        due.forEach { d ->
            Card {
                Body(d.medicine.name + if (d.medicine.dose.isBlank()) "" else " " + d.medicine.dose)
                Caption(showMillis(d.at))
                if (d.status == null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PrimaryButton(stringResource(R.string.r3_dose_taken), { vm.logDose(d.medicine.id, d.at, "taken") }, Modifier.weight(1f))
                        SecondaryButton(stringResource(R.string.r3_dose_skip), { vm.logDose(d.medicine.id, d.at, "skipped") }, Modifier.weight(1f))
                    }
                } else {
                    Body(doseStatusText(d.status), color = LocalSimha.current.success)
                }
            }
        }
        SectionTitle(title)
        if (meds.isEmpty() && !adding) Body(stringResource(R.string.r3_meds_empty))
        meds.forEach { m ->
            if (editing?.id == m.id) {
                MedicineForm(m, { vm.saveMedicine(it); editing = null }, { editing = null })
            } else {
                Card {
                    Body(m.name + if (m.dose.isBlank()) "" else " " + m.dose)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { m.times.forEach { TimeTag(it) } }
                    m.stock?.let { Caption(stringResource(R.string.r3_m_stock_left, it)) }
                    SwitchRow(stringResource(R.string.r3_m_active), m.active, { vm.saveMedicine(m.copy(active = it)) })
                    SecondaryButton(stringResource(R.string.r3_m_edit), { editing = m; adding = false })
                    SecondaryButton(stringResource(R.string.r3_m_delete), { vm.deleteMedicine(m.id) })
                }
            }
        }
        if (adding) {
            MedicineForm(null, { vm.saveMedicine(it); adding = false }, { adding = false })
        } else {
            PrimaryButton(stringResource(R.string.r3_meds_add), { adding = true; editing = null })
        }
    }
}

@Composable
fun doseStatusText(status: String?): String = stringResource(
    when (status) {
        "taken" -> R.string.r3_st_taken
        "skipped" -> R.string.r3_st_skipped
        else -> R.string.r3_st_missed
    }
)
