package com.quizlock.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.ui.QuizLockActivity

class AppLockAccessibilityService : AccessibilityService() {

    private lateinit var prefsManager: PreferencesManager
    private var lastInterceptedPackage: String? = null
    private var lastInterceptTime: Long = 0
    private var lastActiveUnlockedPackage: String? = null

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

        // If user moved to Home Screen or System UI
        if (isHomeOrSystemPackage(pkgName)) {
            // Dismiss active overlay when returning home
            if (QuizOverlayManager.isShowing()) {
                QuizOverlayManager.dismiss()
            }
            // Re-lock previous app session when user returns home
            lastActiveUnlockedPackage?.let {
                prefsManager.lockSession(it)
                lastActiveUnlockedPackage = null
            }
            return
        }

        // Check if this package is locked by user
        if (prefsManager.isAppLocked(pkgName)) {
            // If already in an unlocked session (user currently using it after passing quiz)
            if (prefsManager.isAppInUnlockedSession(pkgName)) {
                lastActiveUnlockedPackage = pkgName
                return
            }

            // User switched to a different locked app or left previous
            if (lastActiveUnlockedPackage != null && lastActiveUnlockedPackage != pkgName) {
                prefsManager.lockSession(lastActiveUnlockedPackage!!)
                lastActiveUnlockedPackage = null
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
        } else {
            // User switched to an unlocked app
            lastActiveUnlockedPackage?.let {
                if (it != pkgName) {
                    prefsManager.lockSession(it)
                    lastActiveUnlockedPackage = null
                }
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
        QuizOverlayManager.dismiss()
    }
}
