package com.simha.app.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.simha.app.MainViewModel
import com.simha.app.data.Snap
import com.simha.app.ui.talk.TalkScreen
import com.simha.app.ui.theme.LocalSimha
import com.simha.app.ui.theme.SimhaTheme

@Composable
fun SimhaRoot(vm: MainViewModel, wsc: WindowWidthSizeClass) {
    val snap by vm.snap.collectAsState()
    val s = snap ?: return
    SimhaTheme(dark = s.night || isSystemInDarkTheme()) {
        ProvideLayout(wsc, s.lang) {
            AppNav(vm, s)
        }
    }
}

@Composable
private fun AppNav(vm: MainViewModel, s: Snap) {
    val nav = rememberNavController()
    val ms = animMs()
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
    val enter: () -> EnterTransition = { if (ms == 0) EnterTransition.None else fadeIn(tween(ms)) + slideInHorizontally(tween(ms)) { it / 16 } }
    val exit: () -> ExitTransition = { if (ms == 0) ExitTransition.None else fadeOut(tween(ms)) + slideOutHorizontally(tween(ms)) { -it / 16 } }
    val popEnter: () -> EnterTransition = { if (ms == 0) EnterTransition.None else fadeIn(tween(ms)) + slideInHorizontally(tween(ms)) { -it / 16 } }
    val popExit: () -> ExitTransition = { if (ms == 0) ExitTransition.None else fadeOut(tween(ms)) + slideOutHorizontally(tween(ms)) { it / 16 } }
    Box(Modifier.fillMaxSize().background(LocalSimha.current.page)) {
    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { enter() },
        exitTransition = { exit() },
        popEnterTransition = { popEnter() },
        popExitTransition = { popExit() },
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
                onSettings = { nav.navigate("settings") },
                onProfile = { nav.navigate("profile") },
            )
        }
        composable("talk/{mode}") { e ->
            TalkScreen(
                checkin = e.arguments?.getString("mode") == "checkin",
                onBack = { if (!nav.popBackStack()) nav.navigate("home") },
                onEmergency = { nav.navigate("emergency") },
                onSettings = { nav.navigate("settings") },
            )
        }
        composable("emergency") { EmergencyScreen(vm, s) { nav.popBackStack() } }
        composable("contacts/{group}") { e ->
            ContactsScreen(vm, s, e.arguments?.getString("group") ?: "family") { nav.popBackStack() }
        }
        composable("profile") { ProfileScreen(vm) { nav.popBackStack() } }
        composable("settings") {
            SettingsScreen(
                vm, s,
                onBack = { nav.popBackStack() },
                onAudit = { nav.navigate("audit") },
                onContacts = { nav.navigate("contacts/$it") },
                onProfile = { nav.navigate("profile") },
            )
        }
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
}
