package com.simha.app.ui

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
import com.simha.app.care.GNode
import com.simha.app.care.NodeType
import kotlinx.coroutines.launch

@Composable
fun KnowsScreen(vm: MainViewModel, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<GNode>>(emptyList()) }
    val reload: () -> Unit = { scope.launch { items = vm.sl.graph.all() } }
    LaunchedEffect(Unit) { reload() }
    val title = stringResource(R.string.cf_knows_title)
    ScreenFrame(title, title, onBack) {
        Body(stringResource(R.string.cf_knows_hint))
        if (items.isEmpty()) Body(stringResource(R.string.cf_knows_empty))
        items.forEach { n ->
            Card {
                Body(n.label)
                Caption(NodeType.of(n.type).word)
                SecondaryButton(stringResource(R.string.cf_knows_forget), {
                    scope.launch {
                        vm.sl.graph.delete(n.id)
                        reload()
                    }
                })
            }
        }
    }
}
