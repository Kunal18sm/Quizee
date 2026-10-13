package com.quizlock.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.service.AppLockForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = PreferencesManager.getInstance(context)
            if (prefs.getLockedPackages().isNotEmpty()) {
                AppLockForegroundService.startService(context)
            }
        }
    }
}
