package com.quizlock.app.data

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "quiz_lock_prefs"
        private const val KEY_LOCKED_APPS = "locked_apps"
        private const val KEY_QUESTIONS_COUNT = "questions_count"
        private const val KEY_UNLOCK_TIMEOUT_MINUTES = "unlock_timeout_minutes"
        
        // In-memory unlocked session cache: packageName -> expirationTimestamp
        private val unlockedSessions = mutableMapOf<String, Long>()

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }

    fun getLockedPackages(): MutableSet<String> {
        val set = prefs.getStringSet(KEY_LOCKED_APPS, emptySet()) ?: emptySet()
        return HashSet(set)
    }

    fun isAppLocked(packageName: String): Boolean {
        return getLockedPackages().contains(packageName)
    }

    fun setAppLocked(packageName: String, locked: Boolean) {
        val current = getLockedPackages()
        if (locked) {
            current.add(packageName)
        } else {
            current.remove(packageName)
            unlockedSessions.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_LOCKED_APPS, current).apply()
    }

    fun getQuestionsPerQuiz(): Int {
        return prefs.getInt(KEY_QUESTIONS_COUNT, 5)
    }

    fun setQuestionsPerQuiz(count: Int) {
        prefs.edit().putInt(KEY_QUESTIONS_COUNT, count).apply()
    }

    fun getUnlockTimeoutMinutes(): Int {
        return prefs.getInt(KEY_UNLOCK_TIMEOUT_MINUTES, 10)
    }

    fun markAppUnlocked(packageName: String) {
        val durationMillis = getUnlockTimeoutMinutes() * 60 * 1000L
        val expiry = System.currentTimeMillis() + durationMillis
        unlockedSessions[packageName] = expiry
    }

    fun isAppInUnlockedSession(packageName: String): Boolean {
        val expiry = unlockedSessions[packageName] ?: return false
        if (System.currentTimeMillis() < expiry) {
            return true
        } else {
            unlockedSessions.remove(packageName)
            return false
        }
    }

    fun lockAllSessions() {
        unlockedSessions.clear()
    }
}
