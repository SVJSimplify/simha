package com.simha.app.update

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.simha.app.R
import com.simha.app.ui.Body
import com.simha.app.ui.Caption
import com.simha.app.ui.Card
import com.simha.app.ui.PrimaryButton
import com.simha.app.ui.SecondaryButton
import com.simha.app.ui.theme.LocalSimha

@Composable
fun UpdateBanner() {
    val ctx = LocalContext.current
    UpdateState.attach(ctx)
    LaunchedEffect(Unit) { UpdateState.checkIfDue() }
    val s by UpdateState.ui.collectAsState()
    val info = s.info
    if (info == null && s.error == null) return
    Card(Modifier.fillMaxWidth()) {
        if (info != null) {
            Body(stringResource(R.string.upd_available, info.versionCode))
            if (info.notes.isNotBlank()) Caption(info.notes.take(300), Modifier.padding(top = 4.dp))
            if (s.busy) {
                LinearProgressIndicator(
                    progress = { s.progress },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    color = LocalSimha.current.primary,
                )
                Caption(stringResource(R.string.upd_downloading, (s.progress * 100).toInt()))
            } else if (s.installed) {
                Body(stringResource(R.string.upd_installed))
            } else {
                PrimaryButton(
                    stringResource(R.string.upd_now),
                    { UpdateState.install() },
                    Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            }
        }
        s.error?.let {
            Body(it, Modifier.padding(top = 8.dp), LocalSimha.current.emergency)
            if (info == null) {
                SecondaryButton(
                    stringResource(R.string.upd_retry),
                    { UpdateState.checkNow() },
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        }
    }
}
