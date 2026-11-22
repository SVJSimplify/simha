package com.simha.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import com.simha.app.ui.SimhaRoot

class MainActivity : AppCompatActivity() {
    private val vm: MainViewModel by viewModels()

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) vm.handleIntent(intent)
        setContent {
            SimhaRoot(vm, calculateWindowSizeClass(this).widthSizeClass)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        vm.onResume()
    }

    companion object {
        const val ACTION_CHECKIN = "com.simha.app.CHECKIN"
        const val ACTION_ESCALATE = "com.simha.app.ESCALATE"
        const val EXTRA_NAME = "name"
        const val EXTRA_NUMBER = "number"
    }
}
