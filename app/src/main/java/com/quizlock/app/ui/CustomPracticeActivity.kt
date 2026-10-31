package com.quizlock.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.quizlock.app.R
import com.quizlock.app.data.PracticeRepository
import com.quizlock.app.data.PracticeSet
import com.quizlock.app.data.PracticeSubject
import com.quizlock.app.data.PracticeTopic
import com.quizlock.app.databinding.ActivityCustomPracticeBinding

class CustomPracticeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomPracticeBinding
    private lateinit var practiceRepo: PracticeRepository
    private lateinit var topicAdapter: PracticeTopicAdapter

    private var allSubjects: List<PracticeSubject> = emptyList()
    private var allTopics: List<PracticeTopic> = emptyList()
    private var currentFilteredTopics: List<PracticeTopic> = emptyList()
    private var currentSubjectFilter: String = "All"
    private var selectedMultiCount: Int = 50

    private val subjectChipMap = mutableMapOf<MaterialButton, String>()
    private val countButtonMap = mutableMapOf<MaterialButton, Int>()

    companion object {
        const val EXTRA_INITIAL_SUBJECT = "initial_subject"

        fun start(context: Context, initialSubject: String? = null) {
            val intent = Intent(context, CustomPracticeActivity::class.java).apply {
                if (initialSubject != null) putExtra(EXTRA_INITIAL_SUBJECT, initialSubject)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomPracticeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        practiceRepo = PracticeRepository.getInstance(this)

        setupToolbar()
        setupMockPromoBanner()
        setupSubjectChips()
        setupMultiSelectControls()
        setupRecyclerView()
        loadData()

        val initialSubject = intent.getStringExtra(EXTRA_INITIAL_SUBJECT)
        if (!initialSubject.isNullOrBlank()) {
            selectSubjectFilter(initialSubject)
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupMockPromoBanner() {
        binding.cardPromoMockPortal.setOnClickListener {
            MockHubActivity.start(this)
        }
        binding.btnOpenMockPortalFromCustom.setOnClickListener {
            MockHubActivity.start(this)
        }
    }

    private fun setupSubjectChips() {
        subjectChipMap[binding.chipAll] = "All"
        subjectChipMap[binding.chipEnglish] = "English"
        subjectChipMap[binding.chipStaticGk] = "Static GK"
        subjectChipMap[binding.chipHistory] = "History"
        subjectChipMap[binding.chipGeography] = "Geography"
        subjectChipMap[binding.chipBiology] = "Biology"
        subjectChipMap[binding.chipPolity] = "Polity"
        subjectChipMap[binding.chipEconomics] = "Economics"

        subjectChipMap.forEach { (btn, subject) ->
            btn.setOnClickListener {
                selectSubjectFilter(subject)
            }
        }
    }

    private fun selectSubjectFilter(subject: String) {
        currentSubjectFilter = subject

        subjectChipMap.forEach { (btn, sub) ->
            if (sub == subject) {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
                btn.strokeWidth = 0
            } else {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.card_dark_elevated)
                btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                btn.strokeColor = ContextCompat.getColorStateList(this, R.color.border_dark)
                btn.strokeWidth = 2
            }
        }

        filterTopics()
    }

    private fun setupMultiSelectControls() {
        countButtonMap[binding.btnMultiCount25] = 25
        countButtonMap[binding.btnMultiCount50] = 50
        countButtonMap[binding.btnMultiCount100] = 100

        countButtonMap.forEach { (btn, count) ->
            btn.setOnClickListener {
                selectMultiCount(count)
            }
        }

        binding.btnClearSelection.setOnClickListener {
            topicAdapter.clearSelections()
        }

        binding.btnStartMultiMixedTest.setOnClickListener {
            startMixedTest()
        }

        selectMultiCount(50)
    }

    private fun selectMultiCount(count: Int) {
        selectedMultiCount = count

        countButtonMap.forEach { (btn, c) ->
            if (c == count) {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
                btn.setTextColor(ContextCompat.getColor(this, R.color.white))
                btn.strokeColor = ContextCompat.getColorStateList(this, R.color.primary)
            } else {
                btn.backgroundTintList = ContextCompat.getColorStateList(this, R.color.card_dark_elevated)
                btn.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
                btn.strokeColor = ContextCompat.getColorStateList(this, R.color.border_dark)
            }
        }

        binding.btnStartMultiMixedTest.text = "🚀 Start Custom Mixed Test ($selectedMultiCount Qs)"
    }

    private fun setupRecyclerView() {
        topicAdapter = PracticeTopicAdapter(
            topics = emptyList(),
            onSetClick = { set, is50Count ->
                startSingleSetTest(set, is50Count = is50Count)
            },
            onTestAllTopicClick = { topic ->
                startFullTopicTest(topic)
            },
            onSelectionChanged = { selectedCategories ->
                updateMultiSelectDock(selectedCategories)
            }
        )

        binding.rvPracticeTopics.layoutManager = LinearLayoutManager(this)
        binding.rvPracticeTopics.adapter = topicAdapter
    }

    private fun loadData() {
        allSubjects = practiceRepo.getCurriculum()
        allTopics = allSubjects.flatMap { it.topics }

        updateResumeCard()
        filterTopics()
    }

    private fun updateResumeCard() {
        val last = practiceRepo.getLastPracticed()
        if (last != null && last.questionIds.isNotEmpty()) {
            binding.cardResume.visibility = View.VISIBLE
            binding.tvResumeTitle.text = last.title
            binding.tvResumeSubtitle.text = "${last.total} Questions • Last Score: ${last.score}/${last.total} (${last.accuracyPercent}%)"
            binding.tvResumeAccuracy.text = "${last.accuracyPercent}% Score"

            binding.btnResumeSet.setOnClickListener {
                PracticeTestActivity.startWithQuestions(
                    context = this,
                    questionIds = last.questionIds.toLongArray(),
                    title = last.title,
                    subtitle = last.subtitle,
                    setKey = last.setKey
                )
            }
        } else {
            binding.cardResume.visibility = View.GONE
        }
    }

    private fun filterTopics() {
        currentFilteredTopics = if (currentSubjectFilter == "All") {
            allTopics
        } else {
            allTopics.filter { it.subjectName == currentSubjectFilter }
        }

        val totalQCount = currentFilteredTopics.sumOf { it.totalQuestions }
        binding.tvSectionHeader.text = if (currentSubjectFilter == "All") "All Topics & Sets" else "$currentSubjectFilter Topics & Sets"
        binding.tvTopicsCountBadge.text = "${currentFilteredTopics.size} Topics • $totalQCount Qs"

        topicAdapter.updateTopics(currentFilteredTopics)
    }

    private fun updateMultiSelectDock(selectedCategories: Set<String>) {
        if (selectedCategories.isNotEmpty()) {
            binding.cardMultiSelectDock.visibility = View.VISIBLE
            val selectedTopics = allTopics.filter { it.categoryName in selectedCategories }
            val poolSize = selectedTopics.sumOf { it.totalQuestions }
            binding.tvMultiSelectSummary.text = "Selected: ${selectedCategories.size} Topics (Pool: $poolSize Questions)"
        } else {
            binding.cardMultiSelectDock.visibility = View.GONE
        }
    }

    private fun startSingleSetTest(set: PracticeSet, is50Count: Boolean) {
        val qIds = if (is50Count && set.questionIds.size > 50) {
            set.questionIds.shuffled().take(50).toLongArray()
        } else {
            set.questionIds.toLongArray()
        }

        val title = if (is50Count && set.questionIds.size > 50) {
            "${set.topicDisplayName} - Set ${set.setNumber} (50 Qs Drill)"
        } else {
            "${set.topicDisplayName} - Set ${set.setNumber}"
        }

        val subtitle = "${qIds.size} Questions • Set ${set.setNumber}"

        PracticeTestActivity.startWithQuestions(
            context = this,
            questionIds = qIds,
            title = title,
            subtitle = subtitle,
            setKey = if (!is50Count) set.setKey else "${set.setKey}_50"
        )
    }

    private fun startFullTopicTest(topic: PracticeTopic) {
        val allIds = topic.sets.flatMap { it.questionIds }
        val sampleSize = allIds.size.coerceAtMost(50)
        val sampledIds = if (allIds.size > sampleSize) allIds.shuffled().take(sampleSize) else allIds

        val title = "${topic.displayName} (50 Qs Drill)"
        val subtitle = "${topic.subjectName} • Whole Topic Pool"
        PracticeTestActivity.startWithQuestions(
            context = this,
            questionIds = sampledIds.toLongArray(),
            title = title,
            subtitle = subtitle,
            setKey = "topic_${topic.categoryName.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()}"
        )
    }

    private fun startMixedTest() {
        val selected = topicAdapter.selectedCategories.toList()
        if (selected.isEmpty()) {
            Toast.makeText(this, "Please select at least 1 topic!", Toast.LENGTH_SHORT).show()
            return
        }

        val questions = practiceRepo.getQuestionsForCategories(selected, selectedMultiCount)
        if (questions.isEmpty()) {
            Toast.makeText(this, "No questions found for selected topics!", Toast.LENGTH_SHORT).show()
            return
        }

        val questionIds = questions.map { it.id }.toLongArray()
        val title = "Custom Mixed Mock (${questions.size} Qs)"
        val subtitle = "Selected from ${selected.size} Topics"

        PracticeTestActivity.startWithQuestions(
            context = this,
            questionIds = questionIds,
            title = title,
            subtitle = subtitle,
            setKey = "custom_mixed_${System.currentTimeMillis()}"
        )
    }
}
