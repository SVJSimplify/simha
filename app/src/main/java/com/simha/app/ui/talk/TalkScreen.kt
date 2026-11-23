package com.simha.app.ui.talk

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simha.app.R
import com.simha.app.ui.BigButton
import com.simha.app.ui.Body
import com.simha.app.ui.Card
import com.simha.app.ui.Kind
import com.simha.app.ui.ScreenFrame
import com.simha.app.wake.WakeReason

@Composable
fun TalkScreen(
    mode: String,
    onBack: () -> Unit,
    onEmergency: () -> Unit,
    onSettings: () -> Unit,
    vm: TalkViewModel = viewModel(),
) {
    val checkin = mode == "checkin"
    val title = stringResource(if (checkin) R.string.ci_q else R.string.talk_title)
    LaunchedEffect(Unit) { vm.refreshConfig() }

    ScreenFrame(title, title, { vm.stop(); onBack() }) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!vm.configured) {
                Card {
                    Body(stringResource(R.string.talk_not_set))
                    BigButton(stringResource(R.string.talk_open_settings), onSettings, kind = Kind.Secondary)
                }
            }
            ConversationPanel(
                reason = if (checkin) WakeReason.Checkin else null,
                extra = null,
                onDone = { if (checkin) onBack() },
                onEmergency = onEmergency,
                unwell = mode == "unwell",
                vm = vm,
            )
            if (checkin || !vm.configured) {
                if (vm.checkedIn) {
                    Body(stringResource(R.string.ci_thanks), color = MaterialTheme.colorScheme.tertiary)
                } else {
                    BigButton(stringResource(R.string.ci_fine), { vm.fine() }, kind = Kind.Success)
                    BigButton(stringResource(R.string.ci_help), { vm.help() }, kind = Kind.Emergency)
                }
            }
            if (!checkin) {
                BigButton(stringResource(R.string.help), { vm.stop(); onEmergency() }, kind = Kind.Emergency)
            }
        }
    }
}
