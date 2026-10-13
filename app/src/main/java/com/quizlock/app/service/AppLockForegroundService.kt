package com.quizlock.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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

    companion object {
        private const val CHANNEL_ID = "quiz_lock_monitor_channel"
        private const val NOTIFICATION_ID = 1001
        private const val POLL_INTERVAL_MS = 1000L

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
        val startTime = endTime - 3000

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        if (!stats.isNullOrEmpty()) {
            val mostRecent = stats.maxByOrNull { it.lastTimeUsed }
            val currentPkg = mostRecent?.packageName ?: return

            if (currentPkg != packageName && currentPkg != "com.android.systemui") {
                if (prefsManager.isAppLocked(currentPkg) &&
                    !prefsManager.isAppInUnlockedSession(currentPkg) &&
                    currentPkg != lastForegroundPackage
                ) {
                    lastForegroundPackage = currentPkg
                    val lockIntent = Intent(this, QuizLockActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(QuizLockActivity.EXTRA_LOCKED_PACKAGE_NAME, currentPkg)
                    }
                    startActivity(lockIntent)
                }
            }
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
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
