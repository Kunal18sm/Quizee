package com.quizlock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.ui.QuizLockActivity

class AppLockAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var isRunning: Boolean = false
    }

    private lateinit var prefsManager: PreferencesManager
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                // When screen turns off, re-lock all sessions
                prefsManager.lockAllSessions()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isRunning = true
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(this)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        val pkgName = event.packageName?.toString() ?: return

        // Ignore our own app package
        if (pkgName == packageName) {
            return
        }

        // If user moved to Home Screen or System UI
        if (isHomeOrSystemPackage(pkgName)) {
            // Dismiss active overlay when returning home
            if (QuizOverlayManager.isShowing()) {
                QuizOverlayManager.dismiss()
            }
            return
        }

        // Check if this package is locked by user
        if (prefsManager.isAppLocked(pkgName)) {
            // If already in an unlocked session (user currently using it after passing quiz)
            if (prefsManager.isAppInUnlockedSession(pkgName)) {
                return
            }

            // If overlay is already showing for this exact package, don't recreate
            if (QuizOverlayManager.isShowingFor(pkgName)) {
                return
            }

            val now = System.currentTimeMillis()
            // Debounce rapid duplicate events within 500ms
            if (pkgName == lastInterceptedPackage && (now - lastInterceptTime) < 500) {
                return
            }

            lastInterceptedPackage = pkgName
            lastInterceptTime = now

            // 1. PRIMARY: Show floating window overlay DIRECTLY on top of the locked app
            val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else true

            if (canOverlay) {
                QuizOverlayManager.show(this, pkgName)
            } else {
                // 2. FALLBACK: Isolated floating Activity (taskAffinity="" prevents Quizee from opening)
                val lockIntent = Intent(this, QuizLockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    putExtra(QuizLockActivity.EXTRA_LOCKED_PACKAGE_NAME, pkgName)
                }
                startActivity(lockIntent)
            }
        }
    }

    private fun isHomeOrSystemPackage(pkgName: String): Boolean {
        if (pkgName == "com.android.systemui" || pkgName.startsWith("com.google.android.inputmethod")) {
            return true
        }

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            val homePackage = resolveInfo?.activityInfo?.packageName
            if (pkgName == homePackage) {
                return true
            }
        } catch (e: Exception) {
            // Ignore error
        }

        return false
    }

    override fun onInterrupt() {
        QuizOverlayManager.dismiss()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        QuizOverlayManager.dismiss()
        try {
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }
}
