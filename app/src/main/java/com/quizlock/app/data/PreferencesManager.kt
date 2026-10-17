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
        
        // In-memory unlocked session cache: set of currently unlocked package names.
        // Once an app is unlocked by passing the quiz, it remains unlocked during phone usage
        // until the phone screen is turned off / locked (Intent.ACTION_SCREEN_OFF).
        private val unlockedSessions = java.util.Collections.synchronizedSet(mutableSetOf<String>())

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
        return prefs.getInt(KEY_UNLOCK_TIMEOUT_MINUTES, 5)
    }

    fun markAppUnlocked(packageName: String) {
        unlockedSessions.add(packageName)
    }

    fun isAppInUnlockedSession(packageName: String): Boolean {
        return unlockedSessions.contains(packageName)
    }

    fun lockSession(packageName: String) {
        unlockedSessions.remove(packageName)
    }

    fun lockAllSessions() {
        unlockedSessions.clear()
    }
}
