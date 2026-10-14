package com.quizlock.app.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.quizlock.app.R
import com.quizlock.app.data.PreferencesManager
import com.quizlock.app.data.Question
import com.quizlock.app.data.QuestionRepository
import com.quizlock.app.databinding.ActivityQuizLockBinding

class QuizLockActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuizLockBinding
    private lateinit var prefsManager: PreferencesManager
    private lateinit var questionRepo: QuestionRepository

    private var targetPackageName: String? = null
    private var targetAppName: String = "App"
    private var quizQuestions: List<Question> = emptyList()
    private var currentQuestionIndex = 0
    private var score = 0
    private var isAnswerLocked = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var optionContainers: List<FrameLayout>
    private lateinit var optionBadges: List<TextView>
    private lateinit var optionTexts: List<TextView>
    private lateinit var optionIcons: List<ImageView>

    companion object {
        const val EXTRA_LOCKED_PACKAGE_NAME = "extra_locked_package_name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuizLockBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = PreferencesManager.getInstance(this)
        questionRepo = QuestionRepository.getInstance(this)

        targetPackageName = intent.getStringExtra(EXTRA_LOCKED_PACKAGE_NAME)

        initViews()
        setupBackPressHandler()
        loadTargetAppDetails()
        loadQuizQuestions()
    }

    private fun initViews() {
        optionContainers = listOf(
            binding.btnOption0,
            binding.btnOption1,
            binding.btnOption2,
            binding.btnOption3
        )
        optionBadges = listOf(
            binding.tvOptionBadge0,
            binding.tvOptionBadge1,
            binding.tvOptionBadge2,
            binding.tvOptionBadge3
        )
        optionTexts = listOf(
            binding.tvOptionText0,
            binding.tvOptionText1,
            binding.tvOptionText2,
            binding.tvOptionText3
        )
        optionIcons = listOf(
            binding.ivFeedback0,
            binding.ivFeedback1,
            binding.ivFeedback2,
            binding.ivFeedback3
        )

        for (i in optionContainers.indices) {
            optionContainers[i].setOnClickListener {
                onOptionClicked(i)
            }
        }

        binding.btnExitHome.setOnClickListener {
            exitToHomeScreen()
        }

        binding.btnExitCompleted.setOnClickListener {
            exitToHomeScreen()
        }

        binding.btnOpenApp.setOnClickListener {
            openTargetApp()
        }

        binding.btnRetryQuiz.setOnClickListener {
            onRetryQuiz()
        }
    }

    private fun onRetryQuiz() {
        score = 0
        currentQuestionIndex = 0
        isAnswerLocked = false

        binding.layoutQuizCompleted.visibility = View.GONE
        binding.layoutActiveQuiz.visibility = View.VISIBLE

        displayQuestion(currentQuestionIndex)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // If quiz is not finished, prevent bypass - return to home screen
                exitToHomeScreen()
            }
        })
    }

    private fun exitToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    private fun loadTargetAppDetails() {
        val pkg = targetPackageName ?: return
        try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(pkg, 0)
            targetAppName = pm.getApplicationLabel(appInfo).toString()
            val icon = pm.getApplicationIcon(appInfo)

            binding.tvLockedAppName.text = "$targetAppName is Locked"
            binding.ivLockedAppIcon.setImageDrawable(icon)
            binding.ivLockedAppIcon.imageTintList = null
        } catch (e: PackageManager.NameNotFoundException) {
            targetAppName = "App"
            binding.tvLockedAppName.text = "App Locked"
        }
    }

    private fun loadQuizQuestions() {
        val count = prefsManager.getQuestionsPerQuiz()
        quizQuestions = questionRepo.getRandomQuestions(count)

        if (quizQuestions.isEmpty()) {
            Toast.makeText(this, "No quiz questions found! Unlocking.", Toast.LENGTH_SHORT).show()
            targetPackageName?.let { prefsManager.markAppUnlocked(it) }
            openTargetApp()
            return
        }

        currentQuestionIndex = 0
        score = 0
        binding.progressBar.max = quizQuestions.size
        displayQuestion(currentQuestionIndex)
    }

    private fun displayQuestion(index: Int) {
        if (index >= quizQuestions.size) {
            onQuizCompleted()
            return
        }

        val q = quizQuestions[index]
        isAnswerLocked = false

        binding.tvQuestionProgress.text = "Question ${index + 1} of ${quizQuestions.size}"
        binding.progressBar.progress = index + 1
        binding.tvScoreBadge.text = "Score: $score"

        binding.tvCategory.text = q.category
        binding.tvQuestionText.text = q.question

        val badges = listOf("A", "B", "C", "D")
        for (i in 0 until 4) {
            val optionText = if (i < q.options.size) q.options[i] else ""
            optionTexts[i].text = optionText
            optionBadges[i].text = badges[i]

            // Reset visual style to dark default
            optionContainers[i].background = ContextCompat.getDrawable(this, R.drawable.bg_option_default)
            optionContainers[i].isEnabled = true
            optionIcons[i].visibility = View.GONE
        }
    }

    private fun onOptionClicked(selectedIndex: Int) {
        if (isAnswerLocked) return
        isAnswerLocked = true

        val q = quizQuestions[currentQuestionIndex]
        val correctIndex = q.correctIndex

        // Disable clicks on all options while revealing answer
        for (container in optionContainers) {
            container.isEnabled = false
        }

        triggerHapticFeedback()

        if (selectedIndex == correctIndex) {
            // Correct answer selected -> turns GREEN
            score++
            binding.tvScoreBadge.text = "Score: $score"
            setOptionAsCorrect(selectedIndex)
        } else {
            // Wrong answer selected -> selected turns RED, correct answer turns GREEN
            setOptionAsWrong(selectedIndex)
            if (correctIndex in 0 until 4) {
                setOptionAsCorrect(correctIndex)
            }
        }

        // Wait 1.0s to show clear visual feedback, then proceed
        handler.postDelayed({
            currentQuestionIndex++
            if (currentQuestionIndex < quizQuestions.size) {
                displayQuestion(currentQuestionIndex)
            } else {
                onQuizCompleted()
            }
        }, 1000)
    }

    private fun setOptionAsCorrect(index: Int) {
        optionContainers[index].background = ContextCompat.getDrawable(this, R.drawable.bg_option_correct)
        optionIcons[index].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_check))
        optionIcons[index].imageTintList = ContextCompat.getColorStateList(this, R.color.correct_green)
        optionIcons[index].visibility = View.VISIBLE
    }

    private fun setOptionAsWrong(index: Int) {
        optionContainers[index].background = ContextCompat.getDrawable(this, R.drawable.bg_option_wrong)
        optionIcons[index].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_cross))
        optionIcons[index].imageTintList = ContextCompat.getColorStateList(this, R.color.wrong_red)
        optionIcons[index].visibility = View.VISIBLE
    }

    private fun onQuizCompleted() {
        val total = quizQuestions.size
        val hasPassed = score >= 3

        // Switch to the Completed Screen
        binding.layoutActiveQuiz.visibility = View.GONE
        binding.layoutQuizCompleted.visibility = View.VISIBLE

        if (hasPassed) {
            targetPackageName?.let { pkg ->
                prefsManager.markAppUnlocked(pkg)
            }

            binding.flCompletedBadge.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.correct_green_light)
            binding.ivCompletedIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_unlock))
            binding.ivCompletedIcon.imageTintList =
                ContextCompat.getColorStateList(this, R.color.correct_green)

            binding.tvCompletedTitle.text = "🎉 App Unlocked!"
            binding.tvCompletedSubtitle.text = "Awesome! You scored $score / $total.\n$targetAppName is unlocked."
            binding.tvFinalScore.text = "Score: $score / $total Correct (Passed ✓)"
            binding.tvFinalScore.setTextColor(ContextCompat.getColor(this, R.color.correct_green))

            binding.btnOpenApp.visibility = View.VISIBLE
            binding.btnOpenApp.text = if (targetAppName.isNotBlank() && targetAppName != "App") {
                "OPEN $targetAppName"
            } else {
                "OPEN APP"
            }
            binding.btnRetryQuiz.visibility = View.GONE
        } else {
            binding.flCompletedBadge.backgroundTintList =
                ContextCompat.getColorStateList(this, R.color.wrong_red_light)
            binding.ivCompletedIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_lock))
            binding.ivCompletedIcon.imageTintList =
                ContextCompat.getColorStateList(this, R.color.wrong_red)

            binding.tvCompletedTitle.text = "❌ Need 3/5 to Unlock"
            binding.tvCompletedSubtitle.text = "You scored $score / $total.\nYou need at least 3 correct answers to unlock $targetAppName.\nTap Retry below to revise these same questions!"
            binding.tvFinalScore.text = "Score: $score / $total (Need At Least 3)"
            binding.tvFinalScore.setTextColor(ContextCompat.getColor(this, R.color.wrong_red))

            binding.btnOpenApp.visibility = View.GONE
            binding.btnRetryQuiz.visibility = View.VISIBLE
        }
    }

    private fun openTargetApp() {
        val pkg = targetPackageName
        if (pkg != null) {
            try {
                val launchIntent = packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    )
                    startActivity(launchIntent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        finish()
    }

    private fun triggerHapticFeedback() {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
