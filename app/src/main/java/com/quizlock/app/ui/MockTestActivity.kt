package com.quizlock.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.quizlock.app.R
import com.quizlock.app.data.PracticeRepository
import com.quizlock.app.data.Question
import com.quizlock.app.data.SectionalScore
import com.quizlock.app.data.TranslationHelper
import com.quizlock.app.databinding.ActivityMockTestBinding
import com.quizlock.app.databinding.DialogMockPaletteBinding
import com.quizlock.app.databinding.ItemMockSectionalScoreBinding
import kotlinx.coroutines.launch
import java.util.Locale

class MockTestActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMockTestBinding
    private lateinit var practiceRepo: PracticeRepository
    private lateinit var translationHelper: TranslationHelper

    private var level: Int = 2
    private var mockNumber: Int = 1
    private var isExamMode: Boolean = true

    private var questions: List<Question> = emptyList()
    private val selectedOptions = IntArray(100) { -1 }
    private val markedForReview = BooleanArray(100) { false }
    private var currentIndex: Int = 0

    private var isHindiMode: Boolean = false
    private var isSubmitted: Boolean = false

    private var timer: CountDownTimer? = null
    private val totalDurationMillis: Long = 60 * 60 * 1000L // 60 minutes
    private var timeRemainingMillis: Long = totalDurationMillis

    private lateinit var optionContainers: List<FrameLayout>
    private lateinit var optionBadges: List<TextView>
    private lateinit var optionTexts: List<TextView>
    private lateinit var optionIcons: List<ImageView>

    // Section Ranges within the 100 questions
    data class SectionMeta(
        val key: String,
        val name: String,
        val icon: String,
        val startIndex: Int,
        val count: Int
    ) {
        val endIndex: Int get() = startIndex + count - 1
    }

    private val sectionList = listOf(
        SectionMeta("english", "English", "📖", 0, 30),
        SectionMeta("static_gk", "Static GK", "🏛️", 30, 18),
        SectionMeta("history", "History", "🏺", 48, 14),
        SectionMeta("polity", "Polity", "⚖️", 62, 12),
        SectionMeta("geography", "Geography", "🌍", 74, 10),
        SectionMeta("biology", "Science", "🧬", 84, 10),
        SectionMeta("economics", "Economy", "📈", 94, 6)
    )

    private val sectionButtons = mutableListOf<MaterialButton>()

    companion object {
        const val EXTRA_LEVEL = "extra_level"
        const val EXTRA_MOCK_NUM = "extra_mock_num"
        const val EXTRA_EXAM_MODE = "extra_exam_mode"

        fun start(context: Context, level: Int, mockNumber: Int, isExamMode: Boolean = true) {
            val intent = Intent(context, MockTestActivity::class.java).apply {
                putExtra(EXTRA_LEVEL, level)
                putExtra(EXTRA_MOCK_NUM, mockNumber)
                putExtra(EXTRA_EXAM_MODE, isExamMode)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMockTestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        practiceRepo = PracticeRepository.getInstance(this)
        translationHelper = TranslationHelper.getInstance(this)

        level = intent.getIntExtra(EXTRA_LEVEL, 2).coerceIn(1, 4)
        mockNumber = intent.getIntExtra(EXTRA_MOCK_NUM, 1).coerceIn(1, 5)
        isExamMode = intent.getBooleanExtra(EXTRA_EXAM_MODE, true)

        initViews()
        setupSectionTabs()
        setupBackPressHandler()
        loadMockQuestions()

        if (isExamMode) {
            startCountdownTimer(totalDurationMillis)
        } else {
            binding.tvMockTimer.text = "📖 Practice Mode"
            binding.tvMockTimer.setTextColor(ContextCompat.getColor(this, R.color.primary_light))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
    }

    private fun initViews() {
        optionContainers = listOf(
            binding.btnMockOption0,
            binding.btnMockOption1,
            binding.btnMockOption2,
            binding.btnMockOption3
        )
        optionBadges = listOf(
            binding.tvMockBadge0,
            binding.tvMockBadge1,
            binding.tvMockBadge2,
            binding.tvMockBadge3
        )
        optionTexts = listOf(
            binding.tvMockOptionText0,
            binding.tvMockOptionText1,
            binding.tvMockOptionText2,
            binding.tvMockOptionText3
        )
        optionIcons = listOf(
            binding.ivMockFeedback0,
            binding.ivMockFeedback1,
            binding.ivMockFeedback2,
            binding.ivMockFeedback3
        )

        val meta = practiceRepo.getMockTestMeta(level, mockNumber)
        binding.tvMockTitle.text = "${meta.levelName} • Full Mock $mockNumber"
        binding.tvMockSubtitle.text = "100 Questions • 60 Mins • 200 Marks (+2.0 / -0.5)"

        for (i in optionContainers.indices) {
            optionContainers[i].setOnClickListener {
                onOptionSelected(i)
            }
        }

        binding.btnExitMock.setOnClickListener {
            confirmExit()
        }

        binding.btnTranslateMock.setOnClickListener {
            if (currentIndex in questions.indices) {
                isHindiMode = !isHindiMode
                applyLanguage(questions[currentIndex])
            }
        }

        binding.btnOpenPalette.setOnClickListener {
            openQuestionPalette()
        }

        binding.btnClearResponse.setOnClickListener {
            if (currentIndex in questions.indices) {
                selectedOptions[currentIndex] = -1
                displayQuestion(currentIndex)
            }
        }

        binding.btnMarkReview.setOnClickListener {
            if (currentIndex in questions.indices) {
                markedForReview[currentIndex] = !markedForReview[currentIndex]
                binding.tvMockMarkedBadge.visibility =
                    if (markedForReview[currentIndex]) View.VISIBLE else View.GONE
                Toast.makeText(
                    this,
                    if (markedForReview[currentIndex]) "Marked for review 🏷️" else "Unmarked",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        binding.btnPrevQuestion.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                displayQuestion(currentIndex)
            }
        }

        binding.btnNextQuestion.setOnClickListener {
            if (currentIndex < questions.size - 1) {
                currentIndex++
                displayQuestion(currentIndex)
            }
        }

        binding.btnSubmitMock.setOnClickListener {
            confirmSubmit()
        }

        binding.btnRetakeMock.setOnClickListener {
            binding.layoutMockResult.visibility = View.GONE
            binding.layoutMockActive.visibility = View.VISIBLE
            isSubmitted = false
            for (i in selectedOptions.indices) {
                selectedOptions[i] = -1
                markedForReview[i] = false
            }
            currentIndex = 0
            timeRemainingMillis = totalDurationMillis
            if (isExamMode) startCountdownTimer(totalDurationMillis)
            displayQuestion(currentIndex)
        }

        binding.btnFinishMock.setOnClickListener {
            finish()
        }
    }

    private fun setupSectionTabs() {
        binding.layoutSectionTabs.removeAllViews()
        sectionButtons.clear()

        for (sec in sectionList) {
            val btn = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(com.google.android.material.R.dimen.m3_btn_stroke_small) + 90
                ).apply {
                    marginEnd = 16
                }
                text = "${sec.icon} ${sec.name} (${sec.count})"
                textSize = 11f
                isAllCaps = false
                cornerRadius = 20
                setPadding(24, 0, 24, 0)
                setOnClickListener {
                    jumpToQuestion(sec.startIndex)
                }
            }
            binding.layoutSectionTabs.addView(btn)
            sectionButtons.add(btn)
        }
    }

    private fun updateActiveSectionTab(questionIndex: Int) {
        val activeSection = sectionList.find { questionIndex in it.startIndex..it.endIndex }
        for (i in sectionList.indices) {
            val sec = sectionList[i]
            val btn = sectionButtons.getOrNull(i) ?: continue
            if (sec == activeSection) {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
                btn.strokeWidth = 0
            } else {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.card_dark_elevated)
                btn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                btn.strokeColor = ContextCompat.getColorStateList(this, R.color.border_dark)
                btn.strokeWidth = 2
            }
        }
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.layoutMockResult.visibility == View.VISIBLE || isSubmitted) {
                    finish()
                } else {
                    confirmExit()
                }
            }
        })
    }

    private fun confirmExit() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Exit Mock Test?")
            .setMessage("Are you sure you want to exit? Your attempt will be lost.")
            .setPositiveButton("Exit") { _, _ ->
                timer?.cancel()
                finish()
            }
            .setNegativeButton("Continue Test", null)
            .show()
    }

    private fun loadMockQuestions() {
        questions = practiceRepo.generateLevelMockTest(level, mockNumber)

        if (questions.size < 100) {
            // Fallback safety to ensure exactly 100 questions
            val fallback = mutableListOf<Question>().apply { addAll(questions) }
            val allQs = practiceRepo.getCurriculum().flatMap { it.topics }.flatMap { it.sets }
                .flatMap { practiceRepo.getQuestionsByIds(it.questionIds) }
            for (q in allQs) {
                if (fallback.size >= 100) break
                if (fallback.none { it.id == q.id }) fallback.add(q)
            }
            questions = fallback.take(100)
        }

        currentIndex = 0
        displayQuestion(currentIndex)
    }

    private fun startCountdownTimer(duration: Long) {
        timer?.cancel()
        timer = object : CountDownTimer(duration, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeRemainingMillis = millisUntilFinished
                val totalSec = millisUntilFinished / 1000
                val min = totalSec / 60
                val sec = totalSec % 60
                val formatted = String.format(Locale.getDefault(), "⏱️ %02d:%02d", min, sec)
                binding.tvMockTimer.text = formatted

                if (min < 5) {
                    binding.tvMockTimer.setTextColor(ContextCompat.getColor(this@MockTestActivity, R.color.wrong_red))
                } else {
                    binding.tvMockTimer.setTextColor(ContextCompat.getColor(this@MockTestActivity, R.color.warning_amber))
                }
            }

            override fun onFinish() {
                timeRemainingMillis = 0
                binding.tvMockTimer.text = "⏱️ 00:00"
                Toast.makeText(this@MockTestActivity, "Time's up! Submitting Mock Test...", Toast.LENGTH_LONG).show()
                submitTest()
            }
        }.start()
    }

    private fun displayQuestion(index: Int) {
        if (index !in questions.indices) return

        val question = questions[index]

        // Header counts
        binding.tvMockQuestionNumber.text = "Q. ${index + 1} / 100"
        binding.pbMockProgress.progress = index + 1

        // Current section tag
        val currentSec = sectionList.find { index in it.startIndex..it.endIndex }
        binding.tvMockSectionBadge.text = "${currentSec?.icon ?: "📌"} ${currentSec?.name ?: question.category}"
        binding.tvMockTopicCategory.text = question.category
        binding.tvMockMarkedBadge.visibility = if (markedForReview[index]) View.VISIBLE else View.GONE

        updateActiveSectionTab(index)

        // Previous / Next button states
        binding.btnPrevQuestion.isEnabled = (index > 0)
        binding.btnNextQuestion.text = if (index == questions.size - 1) "Review 🏁" else "Next ▶"

        // Reset option containers
        val chosenOpt = selectedOptions[index]
        for (i in optionContainers.indices) {
            val container = optionContainers[i]
            val badge = optionBadges[i]
            val text = optionTexts[i]
            val icon = optionIcons[i]

            icon.visibility = View.GONE
            if (i < question.options.size) {
                container.visibility = View.VISIBLE
                if (chosenOpt == i) {
                    // Selected state
                    if (!isExamMode) {
                        // Instant feedback in practice mode
                        if (i == question.correctIndex) {
                            container.background = ContextCompat.getDrawable(this, R.drawable.bg_option_correct)
                            badge.setTextColor(ContextCompat.getColor(this, R.color.correct_green))
                            icon.setImageResource(R.drawable.ic_check)
                            icon.setColorFilter(ContextCompat.getColor(this, R.color.correct_green))
                            icon.visibility = View.VISIBLE
                        } else {
                            container.background = ContextCompat.getDrawable(this, R.drawable.bg_option_wrong)
                            badge.setTextColor(ContextCompat.getColor(this, R.color.wrong_red))
                            icon.setImageResource(R.drawable.ic_cross)
                            icon.setColorFilter(ContextCompat.getColor(this, R.color.wrong_red))
                            icon.visibility = View.VISIBLE
                        }
                    } else {
                        // Exam mode selection
                        container.background = ContextCompat.getDrawable(this, R.drawable.bg_palette_current)
                        badge.setTextColor(ContextCompat.getColor(this, R.color.primary_light))
                    }
                    text.setTextColor(ContextCompat.getColor(this, R.color.white))
                } else {
                    // Default unselected state
                    container.background = ContextCompat.getDrawable(this, R.drawable.bg_option_default)
                    badge.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                    text.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                }
            } else {
                container.visibility = View.GONE
            }
        }

        applyLanguage(question)
    }

    private fun applyLanguage(question: Question) {
        if (!isHindiMode) {
            binding.tvMockQuestionText.text = question.question
            for (i in optionContainers.indices) {
                if (i < question.options.size) {
                    optionTexts[i].text = question.options[i]
                }
            }
            binding.btnTranslateMock.text = "अ / A Hindi"
            binding.btnTranslateMock.setTextColor(ContextCompat.getColor(this, R.color.primary_light))
        } else {
            binding.btnTranslateMock.text = "Translating..."
            lifecycleScope.launch {
                val trans = translationHelper.translateQuestion(question)
                if (currentIndex in questions.indices && questions[currentIndex].id == question.id && isHindiMode) {
                    binding.tvMockQuestionText.text = trans.question
                    for (i in optionContainers.indices) {
                        if (i < trans.options.size) {
                            optionTexts[i].text = trans.options[i]
                        }
                    }
                }
                binding.btnTranslateMock.text = "A / अ English"
                binding.btnTranslateMock.setTextColor(ContextCompat.getColor(this@MockTestActivity, R.color.warning_amber))
            }
        }
    }

    private fun onOptionSelected(optionIndex: Int) {
        if (currentIndex !in questions.indices) return
        selectedOptions[currentIndex] = optionIndex
        displayQuestion(currentIndex)
    }

    private fun jumpToQuestion(index: Int) {
        if (index in questions.indices) {
            currentIndex = index
            displayQuestion(currentIndex)
        }
    }

    private fun openQuestionPalette() {
        val dialog = BottomSheetDialog(this)
        val palBinding = DialogMockPaletteBinding.inflate(LayoutInflater.from(this))
        dialog.setContentView(palBinding.root)

        val answeredCount = selectedOptions.count { it != -1 }
        val markedCount = markedForReview.count { it }
        val unattemptedCount = 100 - answeredCount

        palBinding.tvLegendAnswered.text = "Ans: $answeredCount"
        palBinding.tvLegendMarked.text = "Marked: $markedCount"
        palBinding.tvLegendUnattempted.text = "Left: $unattemptedCount"

        val adapter = MockPaletteAdapter(
            totalCount = questions.size,
            selectedOptions = selectedOptions,
            markedForReview = markedForReview,
            currentIndex = currentIndex
        ) { clickedIndex ->
            dialog.dismiss()
            jumpToQuestion(clickedIndex)
        }

        palBinding.rvPaletteGrid.layoutManager = GridLayoutManager(this, 5)
        palBinding.rvPaletteGrid.adapter = adapter

        palBinding.btnClosePalette.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun confirmSubmit() {
        val answeredCount = selectedOptions.count { it != -1 }
        val unattemptedCount = 100 - answeredCount
        val markedCount = markedForReview.count { it }

        val totalSec = timeRemainingMillis / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        val timeStr = String.format(Locale.getDefault(), "%02d:%02d", min, sec)

        val message = StringBuilder().apply {
            append("• Attempted: $answeredCount / 100\n")
            append("• Unattempted: $unattemptedCount / 100\n")
            if (markedCount > 0) append("• Marked for Review: $markedCount\n")
            if (isExamMode) append("• Time Remaining: $timeStr\n\n")
            append("Are you sure you want to finish and view your SSC CGL scorecard?")
        }.toString()

        MaterialAlertDialogBuilder(this)
            .setTitle("Submit Mock Test?")
            .setMessage(message)
            .setPositiveButton("Submit Test 🏁") { _, _ ->
                submitTest()
            }
            .setNegativeButton("Return to Test", null)
            .show()
    }

    private fun submitTest() {
        if (isSubmitted) return
        isSubmitted = true
        timer?.cancel()

        var correctCount = 0
        var wrongCount = 0
        var unattemptedCount = 0

        // Calculate SSC Marks: +2.0 correct, -0.50 wrong
        for (i in questions.indices) {
            val chosen = selectedOptions[i]
            val correct = questions[i].correctIndex
            when {
                chosen == -1 -> unattemptedCount++
                chosen == correct -> correctCount++
                else -> wrongCount++
            }
        }

        val rawScore = (correctCount * 2.0) - (wrongCount * 0.50)
        val netScore = rawScore.coerceAtLeast(0.0)
        val timeTakenSeconds = ((totalDurationMillis - timeRemainingMillis) / 1000).toInt()

        // Save progress to repository
        practiceRepo.saveMockTestResult(
            level = level,
            mockNumber = mockNumber,
            score = netScore,
            correct = correctCount,
            wrong = wrongCount,
            unattempted = unattemptedCount,
            timeTakenSeconds = timeTakenSeconds
        )

        // Switch to Result View
        binding.layoutMockActive.visibility = View.GONE
        binding.layoutMockResult.visibility = View.VISIBLE

        val meta = practiceRepo.getMockTestMeta(level, mockNumber)
        binding.tvResultMockTitle.text = "${meta.levelName} • Full Mock $mockNumber"
        binding.tvResultNetScore.text = String.format(Locale.getDefault(), "%+.2f", netScore)

        binding.tvResultCorrectCount.text = correctCount.toString()
        binding.tvResultWrongCount.text = wrongCount.toString()
        binding.tvResultUnattemptedCount.text = unattemptedCount.toString()

        val attempted = correctCount + wrongCount
        val accuracy = if (attempted > 0) ((correctCount.toFloat() / attempted) * 100).toInt() else 0
        binding.tvResultAccuracyPercent.text = "$accuracy%"

        binding.tvResultVerdictBadge.text = when {
            netScore >= 160.0 -> "🏆 Top 1% Ranker • Outstanding Score!"
            netScore >= 135.0 -> "🎯 Cleared CGL Tier-1 Expected Cutoff!"
            netScore >= 110.0 -> "⚡ Good Performance • Work on Weak Topics"
            else -> "📚 Needs Revision • Retest Foundational Sets"
        }

        // Calculate and render Sectional Breakdown
        populateSectionalBreakdown()
    }

    private fun populateSectionalBreakdown() {
        binding.layoutSectionalBreakdown.removeAllViews()

        for (sec in sectionList) {
            var cor = 0
            var wrg = 0
            var left = 0

            for (i in sec.startIndex..sec.endIndex) {
                if (i in questions.indices) {
                    val chosen = selectedOptions[i]
                    val actual = questions[i].correctIndex
                    when {
                        chosen == -1 -> left++
                        chosen == actual -> cor++
                        else -> wrg++
                    }
                }
            }

            val secScore = (cor * 2.0) - (wrg * 0.50)
            val rowBinding = ItemMockSectionalScoreBinding.inflate(
                layoutInflater,
                binding.layoutSectionalBreakdown,
                true
            )

            rowBinding.tvSectionTitle.text = "${sec.icon} ${sec.name}"
            rowBinding.tvSectionCorrect.text = cor.toString()
            rowBinding.tvSectionWrong.text = wrg.toString()
            rowBinding.tvSectionUnattempted.text = left.toString()
            rowBinding.tvSectionScore.text = String.format(Locale.getDefault(), "%+.2f", secScore)

            if (secScore >= (sec.count * 1.5)) {
                rowBinding.tvSectionScore.setTextColor(ContextCompat.getColor(this, R.color.correct_green))
            } else if (secScore < 0) {
                rowBinding.tvSectionScore.setTextColor(ContextCompat.getColor(this, R.color.wrong_red))
            } else {
                rowBinding.tvSectionScore.setTextColor(ContextCompat.getColor(this, R.color.primary_light))
            }
        }
    }
}
