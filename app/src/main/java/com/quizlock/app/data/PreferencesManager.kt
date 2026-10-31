package com.quizlock.app.data

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "quiz_lock_prefs"
        private const val KEY_LOCKED_APPS = "locked_apps"
        private const val KEY_QUESTIONS_COUNT = "questions_count"
        private const val KEY_UNLOCK_TIMEOUT_MINUTES = "unlock_timeout_minutes"
        
        // In-memory map of unlocked package names to expiration timestamp in milliseconds.
        // Sessions expire either when the timeout passes or immediately when screen turns off.
        private val unlockedSessionTimestamps = ConcurrentHashMap<String, Long>()

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
            unlockedSessionTimestamps.remove(packageName)
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
        return prefs.getInt(KEY_UNLOCK_TIMEOUT_MINUTES, 5)
    }

    fun setUnlockTimeoutMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_UNLOCK_TIMEOUT_MINUTES, minutes).apply()
    }

    fun markAppUnlocked(packageName: String) {
        val timeoutMinutes = getUnlockTimeoutMinutes()
        val expiryTime = if (timeoutMinutes <= 0) {
            Long.MAX_VALUE // Stays unlocked only until screen turns off
        } else {
            System.currentTimeMillis() + (timeoutMinutes * 60 * 1000L)
        }
        unlockedSessionTimestamps[packageName] = expiryTime
    }

    fun isAppInUnlockedSession(packageName: String): Boolean {
        val expiryTime = unlockedSessionTimestamps[packageName] ?: return false
        if (System.currentTimeMillis() > expiryTime) {
            unlockedSessionTimestamps.remove(packageName)
            return false
        }
        return true
    }

    fun lockSession(packageName: String) {
        unlockedSessionTimestamps.remove(packageName)
    }

    fun lockAllSessions() {
        unlockedSessionTimestamps.clear()
    }
}
