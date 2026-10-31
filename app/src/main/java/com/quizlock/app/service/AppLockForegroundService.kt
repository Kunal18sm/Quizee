package com.quizlock.app.service

import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.quizlock.app.MainActivity
import com.quizlock.app.R
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.receiver.BootReceiver
import com.quizlock.app.ui.QuizLockActivity

class AppLockForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefsManager: PreferencesManager
    private var isMonitoring = false
    private var lastForegroundPackage: String? = null
    private var lastInterceptTime: Long = 0

    private val screenStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // When screen turns off, re-lock all sessions and dismiss any active overlay
                    prefsManager.lockAllSessions()
                    QuizOverlayManager.dismiss()
                    lastForegroundPackage = null
                }
                Intent.ACTION_USER_PRESENT, Intent.ACTION_SCREEN_ON -> {
                    // When user turns on phone or unlocks screen, check foreground app immediately
                    checkForegroundApp()
                }
            }
        }
    }

    companion object {
        private const val CHANNEL_ID = "quiz_lock_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 350L

        fun startService(context: Context) {
            val intent = Intent(context, AppLockForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            context.stopService(Intent(context, AppLockForegroundService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager.getInstance(this)
        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(screenStateReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (!isMonitoring) {
            isMonitoring = true
            startMonitoringLoop()
        }

        // Schedule periodic watchdog to keep service alive 24/7 across idle/Doze periods
        BootReceiver.scheduleWatchdog(this)

        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // If app is swiped away from Recents, schedule an immediate restart
        try {
            val restartServiceIntent = Intent(applicationContext, AppLockForegroundService::class.java).also {
                it.setPackage(packageName)
            }
            val restartPendingIntent = PendingIntent.getService(
                applicationContext,
                1001,
                restartServiceIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.set(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 1000,
                restartPendingIntent
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startMonitoringLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isMonitoring) {
                    checkForegroundApp()
                    handler.postDelayed(this, POLL_INTERVAL_MS)
                }
            }
        }, POLL_INTERVAL_MS)
    }

    private fun isDeviceLocked(): Boolean {
        val power = getSystemService(Context.POWER_SERVICE) as? PowerManager
        if (power != null && !power.isInteractive) return true
        val keyguard = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        if (keyguard != null && keyguard.isKeyguardLocked) return true
        return false
    }

    private fun getForegroundPackage(): String? {
        val usageStatsManager =
            getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10000 // 10-second inspection window

        var latestPkg: String? = null
        var latestTimestamp = 0L

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                    !event.className.isNullOrBlank() &&
                    event.timeStamp >= latestTimestamp
                ) {
                    latestPkg = event.packageName
                    latestTimestamp = event.timeStamp
                }
            }
        } catch (e: Exception) {
            latestPkg = null
        }

        // Fallback: If no activity resumed in the 10-second window, check recent usage
        if (latestPkg == null) {
            try {
                val stats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    endTime - 60000,
                    endTime
                )
                latestPkg = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
            } catch (e: Exception) {
                // Ignore
            }
        }

        return latestPkg
    }

    private fun checkForegroundApp() {
        if (isDeviceLocked()) {
            return
        }

        val pkg = getForegroundPackage() ?: return

        // Ignore our own app, system UI (notifications/lockscreen/quick settings), and keyboards
        if (pkg == packageName || 
            pkg == "com.android.systemui" || 
            pkg.startsWith("com.google.android.inputmethod") ||
            pkg.contains("systemui")
        ) {
            return
        }

        // Check if home launcher is active
        if (isHomePackage(pkg)) {
            if (QuizOverlayManager.isShowing()) {
                QuizOverlayManager.dismiss()
            }
            return
        }

        if (prefsManager.isAppLocked(pkg) && !prefsManager.isAppInUnlockedSession(pkg)) {
            if (QuizOverlayManager.isShowingFor(pkg)) {
                return
            }

            val now = System.currentTimeMillis()
            if (pkg == lastForegroundPackage && (now - lastInterceptTime) < 1500) {
                return
            }

            lastForegroundPackage = pkg
            lastInterceptTime = now

            val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else true

            if (canOverlay) {
                QuizOverlayManager.show(this, pkg)
            } else {
                val lockIntent = Intent(this, QuizLockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    putExtra(QuizLockActivity.EXTRA_LOCKED_PACKAGE_NAME, pkg)
                }
                startActivity(lockIntent)
            }
        }
    }

    private fun isHomePackage(pkgName: String): Boolean {
        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }
            val resolveInfo = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
            return pkgName == resolveInfo?.activityInfo?.packageName
        } catch (e: Exception) {
            return false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_running_title))
            .setContentText(getString(R.string.service_running_desc))
            .setSmallIcon(R.drawable.ic_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
        try {
            unregisterReceiver(screenStateReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
        if (prefsManager.getLockedPackages().isNotEmpty()) {
            BootReceiver.scheduleWatchdog(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
