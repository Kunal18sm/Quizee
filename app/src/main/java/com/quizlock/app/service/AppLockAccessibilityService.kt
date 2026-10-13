package com.quizlock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.ui.QuizLockActivity

class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var prefsManager: PreferencesManager
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(this)
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

        // Ignore common system overlays, keyboards, and launcher
        if (pkgName == "com.android.systemui" || pkgName.startsWith("com.google.android.inputmethod")) {
            return
        }

        // Check if this package is locked by user
        if (prefsManager.isAppLocked(pkgName)) {
            // Check if app is in active unlocked session (quiz answered recently)
            if (!prefsManager.isAppInUnlockedSession(pkgName)) {
                val now = System.currentTimeMillis()
                // Debounce rapid duplicate events within 500ms for same package
                if (pkgName == lastInterceptedPackage && (now - lastInterceptTime) < 500) {
                    return
                }

                lastInterceptedPackage = pkgName
                lastInterceptTime = now

                // Launch the Quiz Lock screen overlay
                val lockIntent = Intent(this, QuizLockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra(QuizLockActivity.EXTRA_LOCKED_PACKAGE_NAME, pkgName)
                }
                startActivity(lockIntent)
            }
        }
    }

    override fun onInterrupt() {
        // Accessibility service interrupted
    }
}
