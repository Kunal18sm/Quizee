package com.quizlock.app

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
import androidx.core.content.ContextCompat
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.quizlock.app.data.AppInfo
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.databinding.ActivityMainBinding
import com.quizlock.app.service.AppLockForegroundService
import com.quizlock.app.ui.AppListAdapter
import com.quizlock.app.ui.PracticeTestActivity
import com.quizlock.app.ui.QuestionManageActivity
import com.quizlock.app.ui.QuizLockActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var dailyStudyManager: com.quizlock.app.data.DailyStudyManager
    private lateinit var appAdapter: AppListAdapter
    private val installedApps = mutableListOf<AppInfo>()
    private var selectedPracticeCount: Int = 30

    companion object {
        private const val PAGE_DAILY = 0
        private const val PAGE_PRACTICE = 1
        private const val PAGE_LOCKER = 2
        private const val PAGE_QUESTIONS = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager.getInstance(this)
        dailyStudyManager = com.quizlock.app.data.DailyStudyManager.getInstance(this)

        setupUI()
        setupListeners()
        setupBottomNavigation()
        loadInstalledApps()

        // Start foreground monitor if any apps are locked
        if (prefsManager.getLockedPackages().isNotEmpty()) {
            AppLockForegroundService.startService(this)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatus()
        updateDailyTargetUI()
    }

    private fun setupUI() {
        appAdapter = AppListAdapter { app, isLocked ->
            prefsManager.setAppLocked(app.packageName, isLocked)
            updateLockedCount()
            if (isLocked) {
                AppLockForegroundService.startService(this)
                val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Settings.canDrawOverlays(this)
                } else true
                if (!hasOverlay) {
                    Toast.makeText(
                        this,
                        "Enable 'Display Over Other Apps' permission so Quiz opens on top of ${app.name}!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.rvApps.layoutManager = LinearLayoutManager(this)
        binding.rvApps.adapter = appAdapter

        updatePermissionStatus()
    }

    private fun setupListeners() {
        // Practice Test Count Selection
        val countButtons = listOf(
            Pair(binding.btnCount10, 10),
            Pair(binding.btnCount20, 20),
            Pair(binding.btnCount30, 30),
            Pair(binding.btnCount40, 40),
            Pair(binding.btnCount50, 50)
        )

        fun updateCountSelection(selected: Int) {
            selectedPracticeCount = selected
            countButtons.forEach { (btn, count) ->
                if (count == selected) {
                    btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                    btn.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.card_dark_elevated)
                    btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            }
            binding.btnStartPracticeTest.text = "⚡ Start Practice Test ($selected Questions)"
        }

        countButtons.forEach { (btn, count) ->
            btn.setOnClickListener {
                updateCountSelection(count)
            }
        }

        binding.btnStartPracticeTest.setOnClickListener {
            PracticeTestActivity.start(this, selectedPracticeCount)
        }

        binding.btnOpenCustomPractice.setOnClickListener {
            com.quizlock.app.ui.CustomPracticeActivity.start(this)
        }

        // Initialize 30 questions selected by default
        updateCountSelection(30)

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
            val canOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.canDrawOverlays(this)
            } else true

            if (canOverlay) {
                com.quizlock.app.service.QuizOverlayManager.show(this, packageName)
            } else {
                val testIntent = Intent(this, QuizLockActivity::class.java).apply {
                    putExtra(QuizLockActivity.EXTRA_LOCKED_PACKAGE_NAME, packageName)
                }
                startActivity(testIntent)
            }
        }

        binding.btnQuestions.setOnClickListener {
            startActivity(Intent(this, QuestionManageActivity::class.java))
        }

        binding.btnToggleDailyTarget.setOnClickListener {
            val currentTarget = dailyStudyManager.getTargetSize()
            val newTarget = if (currentTarget == 30) 20 else 30
            dailyStudyManager.setTargetSize(newTarget)
            Toast.makeText(this, "Daily Target updated to $newTarget Questions/Day", Toast.LENGTH_SHORT).show()
            updateDailyTargetUI()
        }

        binding.btnReviewDailyBank.setOnClickListener {
            showReviewDailyBankDialog()
        }

        binding.btnAdvanceNextDay.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle("Advance to Next Day?")
                .setMessage("Are you sure you want to advance to the next day's question bank now?\n\nToday's progress will be saved and you will unlock tomorrow's questions right away.")
                .setPositiveButton("Advance") { _, _ ->
                    dailyStudyManager.advanceToNextDayManually()
                    Toast.makeText(this, "Unlocked Next Day's Question Bank!", Toast.LENGTH_SHORT).show()
                    updateDailyTargetUI()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnStartDailyDrill.setOnClickListener {
            val targetQuestions = dailyStudyManager.getTodayQuestions()
            if (targetQuestions.isNotEmpty()) {
                val qIds = targetQuestions.map { it.id }.toLongArray()
                PracticeTestActivity.startWithQuestions(
                    context = this,
                    questionIds = qIds,
                    title = "🎯 Day ${dailyStudyManager.getDailyProgress().dayNumber} Target Drill",
                    subtitle = "30-Question Daily Syllabus Test"
                )
            } else {
                PracticeTestActivity.start(this, dailyStudyManager.getTargetSize())
            }
        }

        binding.btnReviewMistakesPractice.setOnClickListener {
            val mistakes = dailyStudyManager.getReviewMistakesQuestions()
            if (mistakes.isNotEmpty()) {
                val qIds = mistakes.map { it.id }.toLongArray()
                PracticeTestActivity.startWithQuestions(
                    context = this,
                    questionIds = qIds,
                    title = "❌ Mistakes Practice Drill",
                    subtitle = "${mistakes.size} Questions needing revision"
                )
            } else {
                Toast.makeText(this, "Great job! You have 0 pending mistakes right now 🎉", Toast.LENGTH_SHORT).show()
            }
        }

        val openCustomHub: (View) -> Unit = {
            com.quizlock.app.ui.CustomPracticeActivity.start(this)
        }
        binding.btnSubjectEnglish.setOnClickListener(openCustomHub)
        binding.btnSubjectStaticGk.setOnClickListener(openCustomHub)
        binding.btnSubjectHistory.setOnClickListener(openCustomHub)
        binding.btnSubjectGeography.setOnClickListener(openCustomHub)
        binding.btnSubjectBiology.setOnClickListener(openCustomHub)
        binding.btnSubjectPolity.setOnClickListener(openCustomHub)
        binding.btnSubjectEconomics.setOnClickListener(openCustomHub)

        binding.btnManageQuestionsPage.setOnClickListener {
            startActivity(Intent(this, QuestionManageActivity::class.java))
        }

        binding.btnTestQuizPage.setOnClickListener {
            binding.btnTestQuiz.performClick()
        }

        binding.etSearchApp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                appAdapter.filter(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_daily -> {
                    showPage(PAGE_DAILY)
                    true
                }
                R.id.nav_practice -> {
                    showPage(PAGE_PRACTICE)
                    true
                }
                R.id.nav_locker -> {
                    showPage(PAGE_LOCKER)
                    true
                }
                R.id.nav_questions -> {
                    showPage(PAGE_QUESTIONS)
                    true
                }
                else -> false
            }
        }
        // Default to Daily Target page
        showPage(PAGE_DAILY)
    }

    private fun showPage(page: Int) {
        binding.layoutPageDaily.visibility = if (page == PAGE_DAILY) View.VISIBLE else View.GONE
        binding.layoutPagePractice.visibility = if (page == PAGE_PRACTICE) View.VISIBLE else View.GONE
        binding.layoutPageLocker.visibility = if (page == PAGE_LOCKER) View.VISIBLE else View.GONE
        binding.layoutPageQuestions.visibility = if (page == PAGE_QUESTIONS) View.VISIBLE else View.GONE
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

    private fun updatePermissionStatus() {
        val hasOverlay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(this)
        } else true
        val hasUsage = hasUsageStatsPermission()

        if (hasOverlay) {
            binding.btnPermOverlay.text = "Enabled ✓"
            binding.btnPermOverlay.setTextColor(ContextCompat.getColor(this, R.color.correct_green))
            binding.btnPermOverlay.isEnabled = false
        } else {
            binding.btnPermOverlay.text = "Enable"
            binding.btnPermOverlay.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnPermOverlay.isEnabled = true
        }

        if (hasUsage) {
            binding.btnPermUsage.text = "Enabled ✓"
            binding.btnPermUsage.setTextColor(ContextCompat.getColor(this, R.color.correct_green))
            binding.btnPermUsage.isEnabled = false
        } else {
            binding.btnPermUsage.text = "Enable"
            binding.btnPermUsage.setTextColor(ContextCompat.getColor(this, R.color.white))
            binding.btnPermUsage.isEnabled = true
        }

        // Keep foreground service active if usage permission is granted and apps are locked
        if (hasUsage && prefsManager.getLockedPackages().isNotEmpty()) {
            AppLockForegroundService.startService(this)
        }
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

    private fun updateDailyTargetUI() {
        dailyStudyManager.ensureDailyBankReady()
        val progress = dailyStudyManager.getDailyProgress()
        val configuredTarget = dailyStudyManager.getTargetSize()

        // Daily Target toggle button is ALWAYS visible, enabled, and shows active quota
        binding.btnToggleDailyTarget.text = "$configuredTarget Qs / Day"
        binding.btnToggleDailyTarget.isEnabled = true

        binding.tvDailyStreak.text = "🔥 ${progress.streak} Day Streak"
        binding.tvDailyMasteryCount.text = "${progress.masteredCount} / ${progress.targetSize} Mastered"

        binding.pbDailyMastery.max = progress.targetSize
        binding.pbDailyMastery.progress = progress.masteredCount

        if (progress.isSundayRevision) {
            binding.tvDailyDayTitle.text = "📅 Sunday Mega Revision"
            binding.tvDailyStatusSubtitle.text = "Weekly Revision Test • ${progress.targetSize} Week Questions"
            binding.btnReviewDailyBank.text = "Review Week's ${progress.targetSize} Qs"
            binding.tvDailySubjectBreakdown.text =
                "🎯 Sunday Mega Test: Comprehensive review of all subjects practiced Monday to Saturday!"
        } else {
            binding.tvDailyDayTitle.text = "🎯 Day ${progress.dayNumber} • Target Bank"

            val subtitle = when {
                progress.isFullyMastered -> "🎉 All ${progress.targetSize} questions mastered for today!"
                progress.mistakeCount > 0 -> "⚡ Active in lock quizzes • ${progress.mistakeCount} to revise"
                else -> "Active in your lock screen quizzes (${progress.seenCount}/${progress.targetSize} seen)"
            }
            binding.tvDailyStatusSubtitle.text = subtitle
            binding.btnReviewDailyBank.text = "Review Today's ${progress.targetSize} Qs"

            val quota = dailyStudyManager.getQuotaForTarget(configuredTarget)
            binding.tvDailySubjectBreakdown.text =
                "Eng ${quota.english} • GK ${quota.staticGk} • Hist ${quota.history} • Geo ${quota.geography} • Bio ${quota.biology} • Pol ${quota.polity} • Eco ${quota.economics}"
        }
    }

    private fun showReviewDailyBankDialog() {
        val dialog = android.app.Dialog(this)
        val dialogBinding = com.quizlock.app.databinding.DialogReviewDailyBankBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val progress = dailyStudyManager.getDailyProgress()
        if (progress.isSundayRevision) {
            dialogBinding.tvDialogTitle.text = "📅 Sunday Mega Revision Bank"
            dialogBinding.tvDialogSubtitle.text = "All ${progress.targetSize} questions practiced across this past week"
        } else {
            dialogBinding.tvDialogTitle.text = "🎯 Day ${progress.dayNumber} • Learning Bank"
            dialogBinding.tvDialogSubtitle.text = "These ${progress.targetSize} questions repeat across your lock quizzes today"
        }

        val bank = dailyStudyManager.getTodayBankQuestions()
        val adapter = com.quizlock.app.ui.DailyReviewAdapter(
            masteredIds = dailyStudyManager.getMasteredIds(),
            mistakeIds = dailyStudyManager.getMistakeIds(),
            seenIds = dailyStudyManager.getSeenIds()
        )
        dialogBinding.rvDailyQuestions.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvDailyQuestions.adapter = adapter
        adapter.submitList(bank)

        dialogBinding.btnCloseDialog.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
