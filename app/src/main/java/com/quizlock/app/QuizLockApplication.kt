package com.quizlock.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class QuizLockApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Force permanent Dark Theme across the entire app and all components
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
