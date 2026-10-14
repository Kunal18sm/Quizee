package com.quizlock.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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

        // Start foreground monitor if any apps are locked
        if (prefsManager.getLockedPackages().isNotEmpty()) {
            AppLockForegroundService.startService(this)
        }
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
                AppLockForegroundService.startService(this)
            }
        }

        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = appAdapter

        updatePermissionStatus()
    }

    private fun setupListeners() {
        // Android 13+ Restricted Settings Fix: Open App Info Directly
        binding.btnFixRestrictedSettings.setOnClickListener {
            openAppDetailsSettings()
        }

        binding.btnHelpRestricted.setOnClickListener {
            showRestrictedSettingsDialog()
        }

        binding.btnPermAccessibility.setOnClickListener {
            val hasAccessibility = isAccessibilityServiceEnabled()
            if (!hasAccessibility) {
                try {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "Find 'Quizee'. If disabled/greyed out, use 'Fix Restricted Settings'!",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    openAppDetailsSettings()
                }
            }
        }

        binding.btnPermOverlay.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    startActivity(intent)
                }
            }
        }

        binding.btnPermUsage.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                startActivity(intent)
                Toast.makeText(this, "Find 'Quizee' and allow Usage Access", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    private fun openAppDetailsSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            startActivity(intent)
            Toast.makeText(
                this,
                "Tap 3 dots (⋮) top right -> 'Allow restricted settings'",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open app settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRestrictedSettingsDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Android 13/14/15 Restricted Settings")
            .setMessage(
                "On modern Android phones, apps installed outside Google Play have accessibility temporarily greyed out.\n\n" +
                "How to unlock in 3 quick steps:\n" +
                "1. Tap 'Open Settings' below (opens Quizee App Info).\n" +
                "2. Tap the 3 dots (⋮) in the top-right corner.\n" +
                "3. Tap 'Allow restricted settings' and confirm your PIN/Fingerprint.\n\n" +
                "After that, you can toggle Accessibility ON normally!"
            )
            .setPositiveButton("Open Settings") { _, _ ->
                openAppDetailsSettings()
            }
            .setNegativeButton("Got It", null)
            .show()
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
            // If accessibility is enabled, hide the restricted settings banner!
            binding.cardRestrictedSettings.visibility = View.GONE
        } else {
            binding.btnPermAccessibility.text = "Enable"
            binding.btnPermAccessibility.isEnabled = true
            // If Android 13+ and accessibility not granted, show the helper banner
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                binding.cardRestrictedSettings.visibility = View.VISIBLE
            } else {
                binding.cardRestrictedSettings.visibility = View.GONE
            }
        }

        if (hasOverlay) {
            binding.btnPermOverlay.text = "Enabled ✓"
            binding.btnPermOverlay.isEnabled = false
        } else {
            binding.btnPermOverlay.text = "Enable"
            binding.btnPermOverlay.isEnabled = true
        }

        if (hasUsage) {
            binding.btnPermUsage.text = "Enabled ✓"
            binding.btnPermUsage.isEnabled = false
        } else {
            binding.btnPermUsage.text = "Enable"
            binding.btnPermUsage.isEnabled = true
        }

        // Keep foreground service active if usage or accessibility is ready
        if (hasUsage || hasAccessibility) {
            if (prefsManager.getLockedPackages().isNotEmpty()) {
                AppLockForegroundService.startService(this)
            }
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
