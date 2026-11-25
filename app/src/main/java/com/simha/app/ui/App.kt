package com.simha.app.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import android.content.res.Configuration
import java.util.Locale
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.simha.app.LocaleCtx
import com.simha.app.LocalizedContext
import com.simha.app.MainViewModel
import com.simha.app.R
import com.simha.app.data.Snap
import com.simha.app.ui.meds.MedicalIdScreen
import com.simha.app.ui.meds.MedicinesScreen
import com.simha.app.ui.talk.TalkScreen
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.ui.theme.SimhaRadius
import com.simha.app.ui.theme.SimhaTheme

private val TABS = listOf("home", "health", "settings")

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabSwitch(): Boolean =
    initialState.destination.route in TABS && targetState.destination.route in TABS

@Composable
fun SimhaRoot(vm: MainViewModel, wsc: WindowWidthSizeClass) {
    val snap by vm.snap.collectAsState()
    val s = snap ?: return
    val base = LocalContext.current
    val cfg = LocalConfiguration.current
    LaunchedEffect(s.lang) { LocaleCtx.save(base, s.lang) }
    val lctx = remember(base, s.lang) { LocalizedContext(base, LocaleCtx.wrap(base, s.lang).resources) }
    val lcfg = remember(cfg, s.lang) {
        Configuration(cfg).also {
            val l = Locale.forLanguageTag(s.lang)
            it.setLocale(l)
            it.setLayoutDirection(l)
        }
    }
    CompositionLocalProvider(LocalContext provides lctx, LocalConfiguration provides lcfg) {
        SimhaTheme(dark = s.night || isSystemInDarkTheme()) {
            ProvideLayout(wsc, s.lang) {
                AppNav(vm, s)
            }
        }
    }
}

@Composable
private fun BottomBar(route: String?, onSelect: (String) -> Unit) {
    val c = LocalSimha.current
    val items = listOf(
        Triple("home", R.drawable.ic_home, R.string.r4c_tab_home),
        Triple("health", R.drawable.ic_heart, R.string.r4c_tab_health),
        Triple("settings", R.drawable.ic_settings, R.string.r4c_tab_settings),
    )
    Box(
        Modifier
            .fillMaxWidth()
            .background(c.surface)
            .border(1.dp, c.border)
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { (r, icon, label) ->
                val on = route == r
                val pill by animateColorAsState(if (on) c.fill else c.surface, tween(NavMs), label = "pill")
                val tint by animateColorAsState(if (on) c.primary else c.text2, tween(NavMs), label = "tint")
                Column(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 72.dp)
                        .pressClick({ onSelect(r) }, role = Role.Tab)
                        .semantics { selected = on }
                        .clip(RoundedCornerShape(SimhaRadius))
                        .background(pill)
                        .border(1.dp, if (on) c.border else Color.Transparent, RoundedCornerShape(SimhaRadius))
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(painterResource(icon), null, Modifier.size(32.dp), tint = tint)
                    Text(stringResource(label), style = MaterialTheme.typography.bodySmall, color = tint, textAlign = TextAlign.Center)
                }
            }
        }
    }
}

@Composable
private fun AppNav(vm: MainViewModel, s: Snap) {
    val nav = rememberNavController()
    val ms = animMs()
    val px = with(LocalDensity.current) { 24.dp.roundToPx() }
    val start = remember {
        when {
            s.role == "caregiver" -> if (s.personId != null) "cg_home" else "cg_setup"
            !s.setup -> "onboard"
            else -> "home"
        }
    }
    val route by vm.route.collectAsState()
    LaunchedEffect(route) {
        route?.let {
            nav.navigate(it) { launchSingleTop = true }
            vm.route.value = null
        }
    }
    val entry by nav.currentBackStackEntryAsState()
    val cur = entry?.destination?.route
    val showBar = cur in TABS
    val goTab: (String) -> Unit = { r ->
        if (r != cur) nav.navigate(r) {
            popUpTo("home")
            launchSingleTop = true
        }
    }
    val ease = EmphasizedEasing
    val c = LocalSimha.current
    Column(Modifier.fillMaxSize().background(c.page)) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(c.page)
                .then(if (showBar) Modifier.consumeWindowInsets(WindowInsets.navigationBars) else Modifier),
        ) {
            NavHost(
                navController = nav,
                startDestination = start,
                modifier = Modifier.background(c.page),
                enterTransition = {
                    if (ms == 0) EnterTransition.None
                    else if (tabSwitch()) fadeIn(tween(NavMs, easing = ease))
                    else fadeIn(tween(NavMs, easing = ease)) + slideInHorizontally(tween(NavMs, easing = ease)) { px }
                },
                exitTransition = {
                    if (ms == 0) ExitTransition.None
                    else if (tabSwitch()) fadeOut(tween(NavMs, easing = ease))
                    else fadeOut(tween(NavMs, easing = ease)) + slideOutHorizontally(tween(NavMs, easing = ease)) { -px }
                },
                popEnterTransition = {
                    if (ms == 0) EnterTransition.None
                    else fadeIn(tween(NavMs, easing = ease)) + slideInHorizontally(tween(NavMs, easing = ease)) { -px }
                },
                popExitTransition = {
                    if (ms == 0) ExitTransition.None
                    else fadeOut(tween(NavMs, easing = ease)) + slideOutHorizontally(tween(NavMs, easing = ease)) { px }
                },
            ) {
                composable("onboard") {
                    OnboardingScreen(vm, s) {
                        nav.navigate("home") { popUpTo("onboard") { inclusive = true } }
                    }
                }
                composable("home") {
                    HomeScreen(
                        vm, s,
                        onTalk = { nav.navigate("talk/chat") },
                        onFamily = { nav.navigate("contacts/family") },
                        onDoctor = { nav.navigate("contacts/doctor") },
                        onHelp = { nav.navigate("emergency") },
                        onMeds = { nav.navigate("meds") },
                        onFixWake = { nav.navigate("set/checkins") },
                    )
                }
                composable("health") {
                    HealthScreen(
                        vm,
                        onMeds = { nav.navigate("meds") },
                        onProfile = { nav.navigate("profile") },
                        onMedicalId = { nav.navigate("medid") },
                        onKnows = { nav.navigate("knows") },
                        onUnwell = { nav.navigate("talk/unwell") },
                        onHelp = { nav.navigate("emergency") },
                    )
                }
                composable("settings") { SettingsScreen { nav.navigate("set/$it") } }
                composable("set/{page}") { e ->
                    SettingsPage(
                        vm, s,
                        page = e.arguments?.getString("page") ?: "me",
                        onBack = { nav.popBackStack() },
                        onAudit = { nav.navigate("audit") },
                        onContacts = { nav.navigate("contacts/$it") },
                    )
                }
                composable("talk/{mode}") { e ->
                    TalkScreen(
                        mode = e.arguments?.getString("mode") ?: "chat",
                        onBack = { if (!nav.popBackStack()) nav.navigate("home") },
                        onEmergency = { nav.navigate("emergency") },
                        onSettings = { nav.navigate("set/ai") },
                    )
                }
                composable("emergency") { EmergencyScreen(vm, s, { nav.popBackStack() }, { nav.navigate("medid") }) }
                composable("meds") { MedicinesScreen(vm) { nav.popBackStack() } }
                composable("knows") { KnowsScreen(vm) { nav.popBackStack() } }
                composable("medid") { MedicalIdScreen(vm) { nav.popBackStack() } }
                composable("contacts/{group}") { e ->
                    ContactsScreen(vm, s, e.arguments?.getString("group") ?: "family") { nav.popBackStack() }
                }
                composable("profile") { ProfileScreen(vm) { nav.popBackStack() } }
                composable("audit") { AuditScreen(vm) { nav.popBackStack() } }
                composable("cg_setup") {
                    CaregiverSetupScreen(vm, s) {
                        nav.navigate("cg_home") { popUpTo("cg_setup") { inclusive = true } }
                    }
                }
                composable("cg_home") {
                    CaregiverHomeScreen(vm, s) {
                        vm.clearPerson()
                        nav.navigate("cg_setup") { popUpTo("cg_home") { inclusive = true } }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showBar,
            enter = fadeIn(tween(NavMs, easing = ease)) + expandVertically(tween(NavMs, easing = ease)),
            exit = fadeOut(tween(NavMs, easing = ease)) + shrinkVertically(tween(NavMs, easing = ease)),
        ) { BottomBar(cur, goTab) }
    }
}
