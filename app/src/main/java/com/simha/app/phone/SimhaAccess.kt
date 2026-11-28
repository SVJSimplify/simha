package com.simha.app.phone

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

class SimhaAccess : AccessibilityService() {
    override fun onServiceConnected() {
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: SimhaAccess? = null
            private set

        fun enabled(ctx: Context): Boolean {
            val want = ComponentName(ctx, SimhaAccess::class.java).flattenToString()
            val on = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return on.split(':').any { it.equals(want, true) }
        }

        fun back(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_BACK) == true
        fun home(): Boolean = instance?.performGlobalAction(GLOBAL_ACTION_HOME) == true

        fun readScreen(ownPackage: String): String? {
            val root = instance?.rootInActiveWindow ?: return null
            if (root.packageName?.toString() == ownPackage) return null
            val out = LinkedHashSet<String>()
            fun walk(n: AccessibilityNodeInfo?, depth: Int) {
                if (n == null || depth > 30 || out.size > 60) return
                if (n.isVisibleToUser) {
                    val t = n.text?.toString()?.trim()
                    if (!t.isNullOrEmpty() && t.length > 1) out += t
                    else n.contentDescription?.toString()?.trim()?.takeIf { it.length > 1 }?.let { out += it }
                }
                for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
            }
            walk(root, 0)
            return out.joinToString(". ").take(1500).ifBlank { null }
        }

        private fun find(labels: List<String>): AccessibilityNodeInfo? {
            val root = instance?.rootInActiveWindow ?: return null
            var hit: AccessibilityNodeInfo? = null
            fun walk(n: AccessibilityNodeInfo?, depth: Int) {
                if (n == null || hit != null || depth > 30) return
                val s = (n.contentDescription?.toString() ?: "") + " " + (n.text?.toString() ?: "")
                if (n.isVisibleToUser && labels.any { s.contains(it, ignoreCase = true) }) {
                    var c: AccessibilityNodeInfo? = n
                    while (c != null && !c.isClickable) c = c.parent
                    if (c != null) hit = c
                }
                for (i in 0 until n.childCount) walk(n.getChild(i), depth + 1)
            }
            walk(root, 0)
            return hit
        }

        suspend fun clickWhenShown(labels: List<String>, timeoutMs: Long = 6000): Boolean {
            val end = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < end) {
                find(labels)?.let { return it.performAction(AccessibilityNodeInfo.ACTION_CLICK) }
                delay(400)
            }
            return false
        }

        val videoCallLabels = listOf("Video call", "वीडियो कॉल", "వీడియో కాల్", "ವಿಡಿಯೋ ಕರೆ", "ವೀಡಿಯೊ ಕರೆ", "வீடியோ அழைப்பு")
        val voiceCallLabels = listOf("Voice call", "Audio call", "वॉइस कॉल", "వాయిస్ కాల్", "ಧ್ವನಿ ಕರೆ", "குரல் அழைப்பு")
    }
}
