package com.quizlock.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.quizlock.app.data.AppInfo
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.databinding.ActivityMainBinding
import com.quizlock.app.service.AppLockAccessibilityService
import com.quizlock.app.service.AppLockForegroundService
import com.quizlock.app.ui.AppListAdapter
import com.quizlock.app.ui.QuestionManageActivity
import com.quizlock.app.ui.QuizLockActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var appAdapter: AppListAdapter
    private val installedApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager.getInstance(this)

        setupUI()
        setupListeners()
        loadInstalledApps()
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
    }

    private fun setupUI() {
        appAdapter = AppListAdapter { app, isLocked ->
            prefsManager.setAppLocked(app.packageName, isLocked)
            updateLockedCount()
            if (isLocked) {
                // Ensure foreground service is running
                AppLockForegroundService.startService(this)
            }
        }

        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = appAdapter

        updatePermissionStatus()
    }

    private fun setupListeners() {
        binding.btnPermAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "Find 'Quizee' and turn it ON", Toast.LENGTH_LONG).show()
        }

        binding.btnPermOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        binding.btnPermUsage.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        binding.btnTestQuiz.setOnClickListener {
            val testIntent = Intent(this, QuizLockActivity::class.java).apply {
                putExtra(QuizLockActivity.EXTRA_LOCKED_PACKAGE_NAME, packageName)
            }
            startActivity(testIntent)
        }

        binding.btnQuestions.setOnClickListener {
            startActivity(Intent(this, QuestionManageActivity::class.java))
        }

        binding.etSearchApp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                appAdapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun updatePermissionStatus() {
        val hasAccessibility = isAccessibilityServiceEnabled()
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
        val hasUsage = hasUsageStatsPermission()

        if (hasAccessibility) {
            binding.btnPermAccessibility.text = "Enabled ✓"
            binding.btnPermAccessibility.isEnabled = false
        } else {
            binding.btnPermAccessibility.text = "Grant"
            binding.btnPermAccessibility.isEnabled = true
        }

        if (hasOverlay) {
            binding.btnPermOverlay.text = "Enabled ✓"
            binding.btnPermOverlay.isEnabled = false
        } else {
            binding.btnPermOverlay.text = "Grant"
            binding.btnPermOverlay.isEnabled = true
        }

        if (hasUsage) {
            binding.btnPermUsage.text = "Enabled ✓"
            binding.btnPermUsage.isEnabled = false
        } else {
            binding.btnPermUsage.text = "Grant"
            binding.btnPermUsage.isEnabled = true
        }

        // Hide permission banner if essential permissions are enabled
        if (hasAccessibility && hasOverlay) {
            binding.cardPermissions.visibility = View.GONE
        } else {
            binding.cardPermissions.visibility = View.VISIBLE
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager ?: return false
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
        val expectedServiceName = "${packageName}/${AppLockAccessibilityService::class.java.canonicalName}"
        for (service in enabledServices) {
            if (service.id.equals(expectedServiceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun loadInstalledApps() {
        binding.progressBarLoading.visibility = View.VISIBLE

        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                val pm = packageManager
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }

                val resolveInfoList = pm.queryIntentActivities(intent, 0)
                val lockedSet = prefsManager.getLockedPackages()
                val list = mutableListOf<AppInfo>()

                for (info in resolveInfoList) {
                    val pkgName = info.activityInfo.packageName
                    if (pkgName == packageName) continue // Skip this app

                    val name = info.loadLabel(pm).toString()
                    val icon = info.loadIcon(pm)
                    val isSystem = (info.activityInfo.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                    list.add(
                        AppInfo(
                            name = name,
                            packageName = pkgName,
                            icon = icon,
                            isLocked = lockedSet.contains(pkgName),
                            isSystemApp = isSystem
                        )
                    )
                }

                list.sortBy { it.name.lowercase() }
                list
            }

            installedApps.clear()
            installedApps.addAll(apps)
            appAdapter.submitList(installedApps)
            binding.progressBarLoading.visibility = View.GONE
            updateLockedCount()
        }
    }

    private fun updateLockedCount() {
        val lockedCount = prefsManager.getLockedPackages().size
        binding.tvLockedCount.text = "$lockedCount Apps Protected with Quizee"
    }
}
