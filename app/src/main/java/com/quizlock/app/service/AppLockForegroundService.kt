package com.quizlock.app.service

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
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.quizlock.app.MainActivity
import com.quizlock.app.R
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.ui.QuizLockActivity

class AppLockForegroundService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var prefsManager: PreferencesManager
    private var isMonitoring = false
    private var lastForegroundPackage: String? = null
    private var lastInterceptTime: Long = 0

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                // When screen turns off, re-lock all sessions
                prefsManager.lockAllSessions()
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

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenOffReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        if (!isMonitoring) {
            isMonitoring = true
            startMonitoringLoop()
        }

        return START_STICKY
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

    private fun checkForegroundApp() {
        val usageStatsManager =
            getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return

        val endTime = System.currentTimeMillis()
        val startTime = endTime - 2500

        var currentPkg: String? = null

        // Try UsageEvents first (responsive & accurate)
        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    currentPkg = event.packageName
                }
            }
        } catch (e: Exception) {
            currentPkg = null
        }

        // Fallback to queryUsageStats
        if (currentPkg == null) {
            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startTime,
                endTime
            )
            if (!stats.isNullOrEmpty()) {
                val mostRecent = stats.maxByOrNull { it.lastTimeUsed }
                currentPkg = mostRecent?.packageName
            }
        }

        val pkg = currentPkg ?: return

        // Ignore our own app, system UI, and common system overlays
        if (pkg == packageName || pkg == "com.android.systemui" || pkg.startsWith("com.google.android.inputmethod")) {
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
            unregisterReceiver(screenOffReceiver)
        } catch (e: Exception) {
            // Ignore if not registered
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
