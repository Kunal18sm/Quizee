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
import com.quizlock.app.data.LevelMockTestMeta
import com.quizlock.app.data.PracticeRepository
import com.quizlock.app.data.PracticeSet
import com.quizlock.app.data.PracticeSubject
import com.quizlock.app.data.PracticeTopic
import com.quizlock.app.databinding.ActivityCustomPracticeBinding

class CustomPracticeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomPracticeBinding
    private lateinit var practiceRepo: PracticeRepository
    private lateinit var topicAdapter: PracticeTopicAdapter
    private lateinit var mockAdapter: MockTestAdapter

    private var isMockHubActive: Boolean = true
    private var activeMockLevel: Int = 2

    private var allSubjects: List<PracticeSubject> = emptyList()
    private var allTopics: List<PracticeTopic> = emptyList()
    private var currentFilteredTopics: List<PracticeTopic> = emptyList()
    private var currentSubjectFilter: String = "All"
    private var selectedMultiCount: Int = 50

    private val subjectChipMap = mutableMapOf<MaterialButton, String>()
    private val countButtonMap = mutableMapOf<MaterialButton, Int>()
    private val levelChipMap = mutableMapOf<MaterialButton, Int>()

    data class LevelDescription(
        val name: String,
        val badge: String,
        val desc: String
    )

    private val levelInfo = mapOf(
        1 to LevelDescription("Level 1: Foundation", "Easy", "Direct, high-yield foundational MCQs from all 7 subjects. Recommended for building speed and basic accuracy."),
        2 to LevelDescription("Level 2: Standard CGL", "Moderate", "Standard Tier-1 exam shift level. Balanced across grammar, articles, rivers, five-year plans & static GK."),
        3 to LevelDescription("Level 3: Advanced", "Hard", "In-depth questions, exceptions, tricky vocabulary, advanced polity amendments and microeconomics."),
        4 to LevelDescription("Level 4: Master Pro", "Pro Ranker", "Toughest repeated questions, high-difficulty shifts, and full All-India rank challenge simulator.")
    )

    companion object {
        const val EXTRA_INITIAL_SUBJECT = "initial_subject"
        const val EXTRA_OPEN_MOCKS = "open_mocks"

        fun start(context: Context, initialSubject: String? = null, openMocks: Boolean = false) {
            val intent = Intent(context, CustomPracticeActivity::class.java).apply {
                if (initialSubject != null) putExtra(EXTRA_INITIAL_SUBJECT, initialSubject)
                putExtra(EXTRA_OPEN_MOCKS, openMocks)
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
        setupModeSwitcher()
        setupMockHub()
        setupSubjectChips()
        setupMultiSelectControls()
        setupRecyclerView()
        loadData()

        val initialSubject = intent.getStringExtra(EXTRA_INITIAL_SUBJECT)
        val shouldOpenMocks = intent.getBooleanExtra(EXTRA_OPEN_MOCKS, false)

        if (!initialSubject.isNullOrBlank()) {
            switchMode(isMock = false)
            selectSubjectFilter(initialSubject)
        } else if (shouldOpenMocks) {
            switchMode(isMock = true)
        } else {
            switchMode(isMock = true)
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

    private fun setupModeSwitcher() {
        binding.btnTabFullMocks.setOnClickListener {
            switchMode(isMock = true)
        }
        binding.btnTabTopicSets.setOnClickListener {
            switchMode(isMock = false)
        }
    }

    private fun switchMode(isMock: Boolean) {
        isMockHubActive = isMock
        if (isMock) {
            binding.btnTabFullMocks.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
            binding.btnTabFullMocks.setTextColor(ContextCompat.getColor(this, R.color.white))

            binding.btnTabTopicSets.backgroundTintList = ContextCompat.getColorStateList(this, R.color.card_dark)
            binding.btnTabTopicSets.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            binding.layoutMockHub.visibility = View.VISIBLE
            binding.layoutTopicSets.visibility = View.GONE
            binding.cardMultiSelectDock.visibility = View.GONE
        } else {
            binding.btnTabTopicSets.backgroundTintList = ContextCompat.getColorStateList(this, R.color.primary)
            binding.btnTabTopicSets.setTextColor(ContextCompat.getColor(this, R.color.white))

            binding.btnTabFullMocks.backgroundTintList = ContextCompat.getColorStateList(this, R.color.card_dark)
            binding.btnTabFullMocks.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))

            binding.layoutMockHub.visibility = View.GONE
            binding.layoutTopicSets.visibility = View.VISIBLE
            updateMultiSelectDock(topicAdapter.selectedCategories)
        }
    }

    private fun setupMockHub() {
        mockAdapter = MockTestAdapter(
            mockList = emptyList(),
            onStartExam = { mockMeta ->
                MockTestActivity.start(
                    context = this,
                    level = mockMeta.level,
                    mockNumber = mockMeta.mockNumber,
                    isExamMode = true
                )
            },
            onStartPractice = { mockMeta ->
                MockTestActivity.start(
                    context = this,
                    level = mockMeta.level,
                    mockNumber = mockMeta.mockNumber,
                    isExamMode = false
                )
            }
        )

        binding.rvMockTests.layoutManager = LinearLayoutManager(this)
        binding.rvMockTests.adapter = mockAdapter

        levelChipMap[binding.chipLevel1] = 1
        levelChipMap[binding.chipLevel2] = 2
        levelChipMap[binding.chipLevel3] = 3
        levelChipMap[binding.chipLevel4] = 4

        levelChipMap.forEach { (btn, lvl) ->
            btn.setOnClickListener {
                selectMockLevel(lvl)
            }
        }

        selectMockLevel(2)
    }

    private fun selectMockLevel(level: Int) {
        activeMockLevel = level

        levelChipMap.forEach { (btn, lvl) ->
            if (lvl == level) {
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

        val info = levelInfo[level] ?: levelInfo[2]!!
        binding.tvActiveLevelName.text = info.name
        binding.tvActiveLevelBadge.text = info.badge
        binding.tvActiveLevelDesc.text = info.desc

        val mockList = practiceRepo.getAllMockTestsForLevel(level)
        mockAdapter.updateList(mockList)
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
            onSetClicked = { set ->
                startSingleSetTest(set, is50Count = false)
            },
            onSet50Clicked = { set ->
                startSingleSetTest(set, is50Count = true)
            },
            onTopicClicked = { topic ->
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
        // Refresh Mock tests
        selectMockLevel(activeMockLevel)

        // Refresh Curriculum
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
        if (!isMockHubActive && selectedCategories.isNotEmpty()) {
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
