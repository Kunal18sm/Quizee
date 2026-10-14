package com.quizlock.app.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.content.ContextCompat
import com.quizlock.app.R
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.data.Question
import com.quizlock.app.data.QuestionRepository
import com.quizlock.app.databinding.ActivityQuizLockBinding

object QuizOverlayManager {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var overlayBinding: ActivityQuizLockBinding? = null
    private var currentPackage: String? = null
    private var currentAppName: String = "App"
    private var isShowingState: Boolean = false
    private var appContext: Context? = null

    private var questions: List<Question> = emptyList()
    private var currentQuestionIndex: Int = 0
    private var score: Int = 0
    private var isAnswerLocked: Boolean = false

    fun isShowing(): Boolean = isShowingState

    fun isShowingFor(packageName: String): Boolean {
        return isShowingState && currentPackage == packageName
    }

    fun getCurrentPackage(): String? = currentPackage

    fun show(context: Context, targetPackageName: String) {
        mainHandler.post {
            val appCtx = context.applicationContext
            appContext = appCtx

            // If already showing for this exact package, don't recreate
            if (isShowingState && currentPackage == targetPackageName) {
                return@post
            }

            // Dismiss any existing overlay first
            dismissInternal()

            currentPackage = targetPackageName
            isShowingState = true

            try {
                val themedContext = ContextThemeWrapper(appCtx, R.style.Theme_QuizLock)
                val inflater = LayoutInflater.from(themedContext)
                val binding = ActivityQuizLockBinding.inflate(inflater)
                overlayBinding = binding

                loadTargetAppDetails(appCtx, binding, targetPackageName)
                setupListeners(appCtx, binding, targetPackageName)

                val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    layoutType,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.CENTER
                    softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
                }

                val windowManager = appCtx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
                windowManager.addView(binding.root, params)

                // Handle back key to return to home screen
                binding.root.isFocusableInTouchMode = true
                binding.root.requestFocus()
                binding.root.setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                        exitToHomeScreen(appCtx)
                        true
                    } else {
                        false
                    }
                }

                // Load questions and start quiz
                loadQuizQuestions(appCtx, binding, targetPackageName)

            } catch (e: Exception) {
                e.printStackTrace()
                isShowingState = false
                currentPackage = null
                overlayBinding = null
            }
        }
    }

    private fun loadTargetAppDetails(
        context: Context,
        binding: ActivityQuizLockBinding,
        packageName: String
    ) {
        try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            currentAppName = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)

            binding.tvLockedAppName.text = "$currentAppName is Locked"
            binding.ivLockedAppIcon.setImageDrawable(icon)
            binding.ivLockedAppIcon.imageTintList = null
        } catch (e: PackageManager.NameNotFoundException) {
            currentAppName = "App"
            binding.tvLockedAppName.text = "App Locked"
        }
    }

    private fun setupListeners(
        context: Context,
        binding: ActivityQuizLockBinding,
        packageName: String
    ) {
        val optionContainers = listOf(
            binding.btnOption0,
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3
        )

        for (i in optionContainers.indices) {
            optionContainers[i].setOnClickListener {
                onOptionClicked(context, binding, i, packageName)
            }
        }

        binding.btnExitHome.setOnClickListener {
            exitToHomeScreen(context)
        }

        binding.btnExitCompleted.setOnClickListener {
            exitToHomeScreen(context)
        }

        binding.btnOpenApp.setOnClickListener {
            onOpenAppClicked(context, packageName)
        }
    }

    private fun loadQuizQuestions(
        context: Context,
        binding: ActivityQuizLockBinding,
        packageName: String
    ) {
        val questionRepo = QuestionRepository.getInstance(context)
        val prefsManager = PreferencesManager.getInstance(context)
        val count = prefsManager.getQuestionsPerQuiz()

        questions = questionRepo.getRandomQuestions(count)

        if (questions.isEmpty()) {
            prefsManager.markAppUnlocked(packageName)
            dismiss()
            return
        }

        currentQuestionIndex = 0
        score = 0
        binding.progressBar.max = questions.size
        displayQuestion(context, binding, currentQuestionIndex)
    }

    private fun displayQuestion(
        context: Context,
        binding: ActivityQuizLockBinding,
        index: Int
    ) {
        if (index >= questions.size) {
            onQuizCompleted(context, binding, currentPackage ?: "")
            return
        }

        val q = questions[index]
        isAnswerLocked = false

        binding.tvQuestionProgress.text = "Question ${index + 1} of ${questions.size}"
        binding.progressBar.progress = index + 1
        binding.tvScoreBadge.text = "Score: $score"

        binding.tvCategory.text = q.category
        binding.tvQuestionText.text = q.question

        val optionContainers = listOf(binding.btnOption0, binding.btnOption1, binding.btnOption2, binding.btnOption3)
        val optionBadges = listOf(binding.tvOptionBadge0, binding.tvOptionBadge1, binding.tvOptionBadge2, binding.tvOptionBadge3)
        val optionTexts = listOf(binding.tvOptionText0, binding.tvOptionText1, binding.tvOptionText2, binding.tvOptionText3)
        val optionIcons = listOf(binding.ivFeedback0, binding.ivFeedback1, binding.ivFeedback2, binding.ivFeedback3)

        val badges = listOf("A", "B", "C", "D")
        for (i in 0 until 4) {
            val optionText = if (i < q.options.size) q.options[i] else ""
            optionTexts[i].text = optionText
            optionBadges[i].text = badges[i]

            // Reset visual style
            optionContainers[i].background = ContextCompat.getDrawable(context, R.drawable.bg_option_default)
            optionContainers[i].isEnabled = true
            optionIcons[i].visibility = View.GONE
        }
    }

    private fun onOptionClicked(
        context: Context,
        binding: ActivityQuizLockBinding,
        selectedIndex: Int,
        packageName: String
    ) {
        if (isAnswerLocked) return
        isAnswerLocked = true

        val q = questions[currentQuestionIndex]
        val correctIndex = q.correctIndex

        val optionContainers = listOf(binding.btnOption0, binding.btnOption1, binding.btnOption2, binding.btnOption3)
        val optionIcons = listOf(binding.ivFeedback0, binding.ivFeedback1, binding.ivFeedback2, binding.ivFeedback3)

        // Disable all options while showing feedback
        for (container in optionContainers) {
            container.isEnabled = false
        }

        triggerHapticFeedback(context)

        if (selectedIndex == correctIndex) {
            // Correct answer
            score++
            binding.tvScoreBadge.text = "Score: $score"
            setOptionAsCorrect(context, optionContainers[selectedIndex], optionIcons[selectedIndex])
        } else {
            // Wrong answer
            setOptionAsWrong(context, optionContainers[selectedIndex], optionIcons[selectedIndex])
            if (correctIndex in 0 until 4) {
                setOptionAsCorrect(context, optionContainers[correctIndex], optionIcons[correctIndex])
            }
        }

        // Wait 1.0s to show feedback, then proceed
        mainHandler.postDelayed({
            if (!isShowingState) return@postDelayed
            currentQuestionIndex++
            if (currentQuestionIndex < questions.size) {
                displayQuestion(context, binding, currentQuestionIndex)
            } else {
                onQuizCompleted(context, binding, packageName)
            }
        }, 1000)
    }

    private fun setOptionAsCorrect(context: Context, container: FrameLayout, icon: ImageView) {
        container.background = ContextCompat.getDrawable(context, R.drawable.bg_option_correct)
        icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_check))
        icon.imageTintList = ContextCompat.getColorStateList(context, R.color.correct_green)
        icon.visibility = View.VISIBLE
    }

    private fun setOptionAsWrong(context: Context, container: FrameLayout, icon: ImageView) {
        container.background = ContextCompat.getDrawable(context, R.drawable.bg_option_wrong)
        icon.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_cross))
        icon.imageTintList = ContextCompat.getColorStateList(context, R.color.wrong_red)
        icon.visibility = View.VISIBLE
    }

    private fun onQuizCompleted(
        context: Context,
        binding: ActivityQuizLockBinding,
        packageName: String
    ) {
        // Mark app unlocked regardless of score
        PreferencesManager.getInstance(context).markAppUnlocked(packageName)

        // Switch to Completed View
        binding.layoutActiveQuiz.visibility = View.GONE
        binding.layoutQuizCompleted.visibility = View.VISIBLE

        binding.tvCompletedSubtitle.text = "You've answered all 5 MCQs.\n$currentAppName is unlocked!"
        binding.tvFinalScore.text = "Score: $score / ${questions.size} Correct"
        binding.btnOpenApp.text = if (currentAppName.isNotBlank() && currentAppName != "App") {
            "OPEN ${currentAppName.uppercase()}"
        } else {
            "OPEN APP"
        }
    }

    private fun onOpenAppClicked(context: Context, packageName: String) {
        PreferencesManager.getInstance(context).markAppUnlocked(packageName)

        // Dismiss the floating overlay
        dismiss()

        // Bring target app to front
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exitToHomeScreen(context: Context) {
        dismiss()
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(homeIntent)
    }

    fun dismiss() {
        mainHandler.post {
            dismissInternal()
        }
    }

    private fun dismissInternal() {
        val binding = overlayBinding
        val ctx = appContext
        if (binding != null && ctx != null) {
            try {
                val windowManager = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
                windowManager?.removeView(binding.root)
            } catch (e: Exception) {
                // View may already be detached
            }
        }
        overlayBinding = null
        isShowingState = false
        currentPackage = null
        mainHandler.removeCallbacksAndMessages(null)
    }

    private fun triggerHapticFeedback(context: Context) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50L, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50L)
            }
        } catch (e: Exception) {
            // Vibration permission or device unsupported
        }
    }
}
