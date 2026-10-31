package com.quizlock.app.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.service.AppLockForegroundService

class BootReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_WATCHDOG_CHECK = "com.quizlock.app.ACTION_WATCHDOG_CHECK"
        private const val WATCHDOG_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        fun scheduleWatchdog(context: Context) {
            try {
                val intent = Intent(context, BootReceiver::class.java).apply {
                    action = ACTION_WATCHDOG_CHECK
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    2002,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                alarmManager?.setInexactRepeating(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + WATCHDOG_INTERVAL_MS,
                    WATCHDOG_INTERVAL_MS,
                    pendingIntent
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == ACTION_WATCHDOG_CHECK
        ) {
            val prefs = PreferencesManager.getInstance(context)
            if (prefs.getLockedPackages().isNotEmpty()) {
                AppLockForegroundService.startService(context)
                scheduleWatchdog(context)
            }
        }
    }
}
