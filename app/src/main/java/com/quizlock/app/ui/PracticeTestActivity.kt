package com.quizlock.app.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.quizlock.app.R
import com.quizlock.app.data.DailyStudyManager
import com.quizlock.app.data.Question
import com.quizlock.app.data.QuestionRepository
import com.quizlock.app.databinding.ActivityPracticeTestBinding

class PracticeTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPracticeTestBinding
    private lateinit var questionRepo: QuestionRepository
    private lateinit var dailyStudyManager: DailyStudyManager

    private var targetCount: Int = 30
    private var questions: List<Question> = emptyList()
    private var currentIndex: Int = 0
    private var score: Int = 0
    private var isAnswerLocked: Boolean = false
    private val reviewResults = mutableListOf<PracticeReviewItem>()

    private lateinit var optionContainers: List<FrameLayout>
    private lateinit var optionBadges: List<TextView>
    private lateinit var optionTexts: List<TextView>
    private lateinit var optionIcons: List<ImageView>

    companion object {
        const val EXTRA_QUESTION_COUNT = "extra_question_count"

        fun start(context: Context, questionCount: Int) {
            val intent = Intent(context, PracticeTestActivity::class.java).apply {
                putExtra(EXTRA_QUESTION_COUNT, questionCount)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPracticeTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        questionRepo = QuestionRepository.getInstance(this)
        dailyStudyManager = DailyStudyManager.getInstance(this)

        targetCount = intent.getIntExtra(EXTRA_QUESTION_COUNT, 30).coerceIn(5, 100)

        initViews()
        setupBackPressHandler()
        loadQuestions()
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

        binding.btnExitTest.setOnClickListener {
            confirmExit()
        }

        binding.btnSkipQuestion.setOnClickListener {
            onSkipClicked()
        }

        binding.btnNextQuestion.setOnClickListener {
            onNextClicked()
        }

        binding.btnRetakeTest.setOnClickListener {
            binding.layoutTestResult.visibility = View.GONE
            binding.layoutQuizActive.visibility = View.VISIBLE
            loadQuestions()
        }

        binding.btnFinishTest.setOnClickListener {
            finish()
        }

        binding.rvReviewQuestions.layoutManager = LinearLayoutManager(this)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.layoutTestResult.visibility == View.VISIBLE) {
                    finish()
                } else {
                    confirmExit()
                }
            }
        })
    }

    private fun confirmExit() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Exit Practice Test?")
            .setMessage("Are you sure you want to quit? Your current test progress will not be saved.")
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .setNegativeButton("Continue", null)
            .show()
    }

    private fun loadQuestions() {
        // Guaranteed starting & active foundational questions
        questions = questionRepo.getPracticeTestQuestions(targetCount)

        if (questions.isEmpty()) {
            Toast.makeText(this, "No practice questions available", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        currentIndex = 0
        score = 0
        reviewResults.clear()

        binding.pbTestProgress.max = questions.size
        displayQuestion(currentIndex)
    }

    private fun displayQuestion(index: Int) {
        if (index !in questions.indices) return

        isAnswerLocked = false
        val question = questions[index]

        // Update progress & header
        binding.tvQuestionProgress.text = "Question ${index + 1} of ${questions.size}"
        binding.pbTestProgress.progress = index + 1
        binding.tvScoreBadge.text = "Score: $score"

        // Update question card
        binding.tvSubjectTag.text = question.category
        binding.tvQuestionText.text = question.question

        // Reset option styling
        for (i in optionContainers.indices) {
            val container = optionContainers[i]
            val badge = optionBadges[i]
            val text = optionTexts[i]
            val icon = optionIcons[i]

            container.background = ContextCompat.getDrawable(this, R.drawable.bg_option_default)
            container.isEnabled = true
            badge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            badge.background = ContextCompat.getDrawable(this, R.drawable.bg_option_default)
            text.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            icon.visibility = View.GONE

            if (i < question.options.size) {
                container.visibility = View.VISIBLE
                text.text = question.options[i]
            } else {
                container.visibility = View.GONE
            }
        }

        // Reset next & skip buttons
        binding.btnSkipQuestion.visibility = View.VISIBLE
        binding.btnNextQuestion.isEnabled = false
        binding.btnNextQuestion.text = if (index == questions.size - 1) "View Results" else "Next Question ➔"
    }

    private fun onOptionClicked(optionIndex: Int) {
        if (isAnswerLocked || currentIndex !in questions.indices) return

        isAnswerLocked = true
        val currentQuestion = questions[currentIndex]
        val isCorrect = (optionIndex == currentQuestion.correctIndex)

        // Disable further clicks
        optionContainers.forEach { it.isEnabled = false }

        if (isCorrect) {
            score++
            binding.tvScoreBadge.text = "Score: $score"
            vibrateDevice(false)

            // Highlight chosen option green
            optionContainers[optionIndex].background =
                ContextCompat.getDrawable(this, R.drawable.bg_option_correct)
            optionTexts[optionIndex].setTextColor(ContextCompat.getColor(this, R.color.white))
            optionBadges[optionIndex].setTextColor(ContextCompat.getColor(this, R.color.correct_green))
            optionIcons[optionIndex].apply {
                setImageResource(R.drawable.ic_check)
                setColorFilter(ContextCompat.getColor(this@PracticeTestActivity, R.color.correct_green))
                visibility = View.VISIBLE
            }
        } else {
            vibrateDevice(true)

            // Highlight chosen option red
            optionContainers[optionIndex].background =
                ContextCompat.getDrawable(this, R.drawable.bg_option_wrong)
            optionTexts[optionIndex].setTextColor(ContextCompat.getColor(this, R.color.white))
            optionBadges[optionIndex].setTextColor(ContextCompat.getColor(this, R.color.wrong_red))
            optionIcons[optionIndex].apply {
                setImageResource(R.drawable.ic_cross)
                setColorFilter(ContextCompat.getColor(this@PracticeTestActivity, R.color.wrong_red))
                visibility = View.VISIBLE
            }

            // Highlight correct option green
            if (currentQuestion.correctIndex in optionContainers.indices) {
                val correctContainer = optionContainers[currentQuestion.correctIndex]
                correctContainer.background =
                    ContextCompat.getDrawable(this, R.drawable.bg_option_correct)
                optionTexts[currentQuestion.correctIndex].setTextColor(
                    ContextCompat.getColor(this, R.color.white)
                )
                optionIcons[currentQuestion.correctIndex].apply {
                    setImageResource(R.drawable.ic_check)
                    setColorFilter(ContextCompat.getColor(this@PracticeTestActivity, R.color.correct_green))
                    visibility = View.VISIBLE
                }
            }
        }

        // Record in review list
        reviewResults.add(
            PracticeReviewItem(
                questionNumber = currentIndex + 1,
                question = currentQuestion,
                selectedIndex = optionIndex,
                isCorrect = isCorrect
            )
        )

        // Count toward daily curriculum mastery!
        dailyStudyManager.recordAnswer(currentQuestion.id, isCorrect)

        // Hide skip, enable next button
        binding.btnSkipQuestion.visibility = View.GONE
        binding.btnNextQuestion.isEnabled = true
    }

    private fun onSkipClicked() {
        if (isAnswerLocked || currentIndex !in questions.indices) return

        isAnswerLocked = true
        val currentQuestion = questions[currentIndex]

        // Disable options
        optionContainers.forEach { it.isEnabled = false }

        // Highlight correct option green
        if (currentQuestion.correctIndex in optionContainers.indices) {
            val correctContainer = optionContainers[currentQuestion.correctIndex]
            correctContainer.background =
                ContextCompat.getDrawable(this, R.drawable.bg_option_correct)
            optionTexts[currentQuestion.correctIndex].setTextColor(
                ContextCompat.getColor(this, R.color.white)
            )
            optionIcons[currentQuestion.correctIndex].apply {
                setImageResource(R.drawable.ic_check)
                setColorFilter(ContextCompat.getColor(this@PracticeTestActivity, R.color.correct_green))
                visibility = View.VISIBLE
            }
        }

        // Record as skipped (-1)
        reviewResults.add(
            PracticeReviewItem(
                questionNumber = currentIndex + 1,
                question = currentQuestion,
                selectedIndex = -1,
                isCorrect = false
            )
        )

        binding.btnSkipQuestion.visibility = View.GONE
        binding.btnNextQuestion.isEnabled = true
    }

    private fun onNextClicked() {
        binding.btnNextQuestion.isEnabled = false
        if (currentIndex + 1 < questions.size) {
            currentIndex++
            displayQuestion(currentIndex)
        } else {
            showTestResults()
        }
    }

    private fun showTestResults() {
        binding.layoutQuizActive.visibility = View.GONE
        binding.layoutTestResult.visibility = View.VISIBLE

        val total = questions.size
        val correct = score
        val wrong = reviewResults.count { !it.isCorrect && it.selectedIndex != -1 }
        val skipped = reviewResults.count { it.selectedIndex == -1 }
        val percentage = if (total > 0) ((correct.toFloat() / total) * 100).toInt() else 0

        binding.tvResultScore.text = "$correct / $total"
        binding.tvResultPercentage.text = "$percentage%"
        binding.tvCountCorrect.text = "$correct"
        binding.tvCountWrong.text = "$wrong"
        binding.tvCountSkipped.text = "$skipped"
        binding.tvResultAccuracy.text = "$percentage%"

        binding.tvResultBadge.text = when {
            percentage >= 85 -> "🏆 Outstanding! Master Level"
            percentage >= 70 -> "🎯 Great Performance! Keep it up"
            percentage >= 50 -> "📖 Good Effort! Needs quick revision"
            else -> "💪 Keep Practicing! Focus on mistakes"
        }

        // Subject Breakdown summary
        val subjectGroups = reviewResults.groupBy { it.question.category.split(":").first().trim() }
        val breakdownText = subjectGroups.entries.joinToString(" • ") { (subject, list) ->
            val subCorrect = list.count { it.isCorrect }
            "$subject: $subCorrect/${list.size}"
        }
        binding.tvSubjectBreakdownSummary.text = if (breakdownText.isNotBlank()) breakdownText else "All starting subjects tested"

        // Set up review adapter
        val adapter = PracticeReviewAdapter(reviewResults)
        binding.rvReviewQuestions.adapter = adapter
    }

    private fun vibrateDevice(isError: Boolean) {
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (isError) {
                    val timings = longArrayOf(0, 80, 60, 80)
                    val amplitudes = intArrayOf(0, 200, 0, 200)
                    vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                }
            } else {
                @Suppress("DEPRECATION")
                if (isError) {
                    vibrator.vibrate(longArrayOf(0, 80, 60, 80), -1)
                } else {
                    vibrator.vibrate(40)
                }
            }
        } catch (e: Exception) {
            // Ignore vibration errors
        }
    }
}
