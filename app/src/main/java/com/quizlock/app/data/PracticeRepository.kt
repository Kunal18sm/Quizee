package com.quizlock.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class PracticeRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_PRACTICE, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val questionRepo = QuestionRepository.getInstance(context)

    companion object {
        private const val PREFS_PRACTICE = "practice_sets_progress"
        private const val KEY_LAST_PRACTICED = "last_practiced_set_json"
        private const val CHUNK_SIZE = 100

        @Volatile
        private var instance: PracticeRepository? = null

        fun getInstance(context: Context): PracticeRepository {
            return instance ?: synchronized(this) {
                instance ?: PracticeRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    /**
     * Builds and returns the comprehensive curriculum structure:
     * 7 Subjects -> Topics -> 100-Question Sets with live saved progress.
     */
    fun getCurriculum(): List<PracticeSubject> {
        val subjects = mutableListOf<PracticeSubject>()

        // 1. English
        subjects.add(buildSubject("English", "📖", questionRepo.englishQuestions))

        // 2. Static GK
        subjects.add(buildSubject("Static GK", "🏛️", questionRepo.staticGkQuestions))

        // 3. Indian History
        subjects.add(buildSubject("History", "🏺", questionRepo.historyQuestions))

        // 4. Geography
        subjects.add(buildSubject("Geography", "🌍", questionRepo.geographyQuestions))

        // 5. Biology
        subjects.add(buildSubject("Biology", "🧬", questionRepo.biologyQuestions))

        // 6. Indian Polity
        subjects.add(buildSubject("Polity", "⚖️", questionRepo.polityQuestions))

        // 7. Economics
        subjects.add(buildSubject("Economics", "📈", questionRepo.economicsQuestions))

        return subjects
    }

    private fun buildSubject(
        subjectName: String,
        iconEmoji: String,
        questions: List<Question>
    ): PracticeSubject {
        // Group by category while preserving order
        val categoryMap = LinkedHashMap<String, MutableList<Question>>()
        for (q in questions) {
            val cat = q.category.ifBlank { subjectName }
            categoryMap.getOrPut(cat) { mutableListOf() }.add(q)
        }

        val topics = mutableListOf<PracticeTopic>()
        for ((catName, qList) in categoryMap) {
            val displayName = cleanCategoryName(catName, subjectName)
            val sets = chunkIntoSets(subjectName, catName, displayName, qList)
            topics.add(
                PracticeTopic(
                    subjectName = subjectName,
                    categoryName = catName,
                    displayName = displayName,
                    totalQuestions = qList.size,
                    sets = sets
                )
            )
        }

        return PracticeSubject(
            name = subjectName,
            iconEmoji = iconEmoji,
            totalQuestions = questions.size,
            topics = topics
        )
    }

    private fun cleanCategoryName(catName: String, subjectName: String): String {
        return when {
            catName.startsWith("English:") -> catName.removePrefix("English:").trim()
            catName.startsWith("Modern History:") -> catName.removePrefix("Modern History:").trim() + " (Modern)"
            catName.startsWith("Medieval History:") -> catName.removePrefix("Medieval History:").trim() + " (Medieval)"
            catName.startsWith("Ancient History:") -> catName.removePrefix("Ancient History:").trim() + " (Ancient)"
            catName == subjectName -> "$subjectName (Complete Bank)"
            else -> catName.trim()
        }
    }

    private fun chunkIntoSets(
        subjectName: String,
        categoryName: String,
        topicDisplayName: String,
        questions: List<Question>
    ): List<PracticeSet> {
        val sets = mutableListOf<PracticeSet>()
        val total = questions.size
        if (total == 0) return emptyList()

        val numSets = (total + CHUNK_SIZE - 1) / CHUNK_SIZE
        for (i in 0 until numSets) {
            val startIndex = i * CHUNK_SIZE
            val endIndex = (startIndex + CHUNK_SIZE).coerceAtMost(total)
            val chunkQuestions = questions.subList(startIndex, endIndex)
            val questionIds = chunkQuestions.map { it.id }

            val setNumber = i + 1
            val sanitizedCat = categoryName.replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
            val setKey = "set_${subjectName.lowercase()}_${sanitizedCat}_$setNumber"

            val isCompleted = prefs.getBoolean("${setKey}_completed", false)
            val score = prefs.getInt("${setKey}_score", 0)
            val timestamp = prefs.getLong("${setKey}_timestamp", 0L)

            sets.add(
                PracticeSet(
                    setKey = setKey,
                    subjectName = subjectName,
                    topicDisplayName = topicDisplayName,
                    categoryName = categoryName,
                    setNumber = setNumber,
                    questionIds = questionIds,
                    startRange = startIndex + 1,
                    endRange = endIndex,
                    score = score,
                    isCompleted = isCompleted,
                    lastAttemptTimestamp = timestamp
                )
            )
        }
        return sets
    }

    /**
     * Saves the test score and completion for a specific set.
     */
    fun saveSetResult(
        setKey: String,
        score: Int,
        total: Int,
        title: String,
        subtitle: String,
        questionIds: List<Long>
    ) {
        val now = System.currentTimeMillis()
        prefs.edit()
            .putBoolean("${setKey}_completed", true)
            .putInt("${setKey}_score", score)
            .putLong("${setKey}_timestamp", now)
            .apply()

        // Also save as Last Practiced Set for the top resume banner
        val info = LastPracticedInfo(
            setKey = setKey,
            title = title,
            subtitle = subtitle,
            score = score,
            total = total,
            timestamp = now,
            questionIds = questionIds
        )
        prefs.edit().putString(KEY_LAST_PRACTICED, gson.toJson(info)).apply()
    }

    fun getLastPracticed(): LastPracticedInfo? {
        val json = prefs.getString(KEY_LAST_PRACTICED, null) ?: return null
        return try {
            gson.fromJson(json, LastPracticedInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getQuestionsByIds(ids: List<Long>): List<Question> {
        val all = questionRepo.getAllQuestions()
        val idMap = all.associateBy { it.id }
        return ids.mapNotNull { idMap[it] }
    }

    /**
     * Samples questions from multiple categories/topics for custom mixed mocks.
     */
    fun getQuestionsForCategories(categories: List<String>, count: Int): List<Question> {
        val all = questionRepo.getAllQuestions()
        val catSet = categories.toSet()
        val pool = all.filter { it.category in catSet }
        if (pool.isEmpty()) return emptyList()
        val shuffled = pool.shuffled()
        return if (shuffled.size > count) shuffled.take(count) else shuffled
    }

    // ========================================================
    // 100-QUESTION LEVEL-WISE MOCK TEST GENERATOR & REPOSITORY
    // ========================================================

    data class MockSectionRule(
        val key: String,
        val name: String,
        val icon: String,
        val count: Int
    )

    private val mockSectionRules = listOf(
        MockSectionRule("english", "English Language", "📖", 30),
        MockSectionRule("static_gk", "Static GK", "🏛️", 18),
        MockSectionRule("history", "Indian History", "🏺", 14),
        MockSectionRule("polity", "Indian Polity", "⚖️", 12),
        MockSectionRule("geography", "Geography", "🌍", 10),
        MockSectionRule("biology", "Science & Biology", "🧬", 10),
        MockSectionRule("economics", "Indian Economy", "📈", 6)
    )

    fun getMockSectionRules(): List<MockSectionRule> = mockSectionRules

    /**
     * Generates a balanced 100-question Mock Test tailored to the requested Level (1..4)
     * and Mock Set Number (1..5).
     */
    fun generateLevelMockTest(level: Int, mockNumber: Int): List<Question> {
        val clampedLevel = level.coerceIn(1, 4)
        val clampedMockNum = mockNumber.coerceIn(1, 15)

        val (startPct, endPct) = when (clampedLevel) {
            1 -> Pair(0.00, 0.25) // Foundation / Direct
            2 -> Pair(0.25, 0.55) // Standard CGL Tier-1
            3 -> Pair(0.55, 0.80) // Advanced
            4 -> Pair(0.80, 1.00) // Master Challenge
            else -> Pair(0.25, 0.55)
        }

        val result100 = mutableListOf<Question>()

        for (rule in mockSectionRules) {
            val fullSubjectPool = when (rule.key) {
                "english" -> questionRepo.englishQuestions
                "static_gk" -> questionRepo.staticGkQuestions
                "history" -> questionRepo.historyQuestions
                "polity" -> questionRepo.polityQuestions
                "geography" -> questionRepo.geographyQuestions
                "biology" -> questionRepo.biologyQuestions
                "economics" -> questionRepo.economicsQuestions
                else -> emptyList()
            }

            if (fullSubjectPool.isEmpty()) continue

            val poolSize = fullSubjectPool.size
            val startIdx = (poolSize * startPct).toInt().coerceIn(0, poolSize - 1)
            val endIdx = (poolSize * endPct).toInt().coerceIn(startIdx + 1, poolSize)
            val slice = if (endIdx > startIdx) fullSubjectPool.subList(startIdx, endIdx) else fullSubjectPool
            val targetPool = if (slice.size >= rule.count) slice else fullSubjectPool

            val quota = rule.count
            val nSub = targetPool.size
            val offset = ((clampedMockNum - 1) * quota) % nSub

            val selected = mutableListOf<Question>()
            for (i in 0 until quota) {
                selected.add(targetPool[(offset + i) % nSub])
            }
            result100.addAll(selected)
        }

        return result100
    }

    fun saveMockTestResult(
        level: Int,
        mockNumber: Int,
        score: Double,
        correct: Int,
        wrong: Int,
        unattempted: Int,
        timeTakenSeconds: Int
    ) {
        val keyPrefix = "mock_lvl_${level}_num_${mockNumber}"
        val now = System.currentTimeMillis()

        prefs.edit()
            .putBoolean("${keyPrefix}_completed", true)
            .putString("${keyPrefix}_score", String.format("%.2f", score))
            .putInt("${keyPrefix}_correct", correct)
            .putInt("${keyPrefix}_wrong", wrong)
            .putInt("${keyPrefix}_unattempted", unattempted)
            .putInt("${keyPrefix}_time", timeTakenSeconds)
            .putLong("${keyPrefix}_timestamp", now)
            .apply()
    }

    fun getMockTestMeta(level: Int, mockNumber: Int): LevelMockTestMeta {
        val clampedLevel = level.coerceIn(1, 4)
        val clampedMockNum = mockNumber.coerceIn(1, 15)

        val (badge, name) = when (clampedLevel) {
            1 -> Pair("Easy", "Level 1: Foundation")
            2 -> Pair("Moderate", "Level 2: Standard CGL")
            3 -> Pair("Hard", "Level 3: Advanced")
            4 -> Pair("Pro Ranker", "Level 4: Master Challenge")
            else -> Pair("Moderate", "Level 2: Standard CGL")
        }

        val keyPrefix = "mock_lvl_${clampedLevel}_num_${clampedMockNum}"
        val isCompleted = prefs.getBoolean("${keyPrefix}_completed", false)
        val scoreStr = prefs.getString("${keyPrefix}_score", "0.0") ?: "0.0"
        val score = scoreStr.toDoubleOrNull() ?: 0.0
        val correct = prefs.getInt("${keyPrefix}_correct", 0)
        val wrong = prefs.getInt("${keyPrefix}_wrong", 0)
        val unattempted = prefs.getInt("${keyPrefix}_unattempted", 0)
        val timeTaken = prefs.getInt("${keyPrefix}_time", 0)
        val timestamp = prefs.getLong("${keyPrefix}_timestamp", 0L)

        return LevelMockTestMeta(
            level = clampedLevel,
            mockNumber = clampedMockNum,
            title = "Full Mock $clampedMockNum",
            levelBadge = badge,
            levelName = name,
            questionCount = 100,
            durationMinutes = 60,
            maxMarks = 200.0,
            isCompleted = isCompleted,
            score = score,
            correctCount = correct,
            wrongCount = wrong,
            unattemptedCount = unattempted,
            timeTakenSeconds = timeTaken,
            lastAttemptTimestamp = timestamp
        )
    }

    fun getAllMockTestsForLevel(level: Int): List<LevelMockTestMeta> {
        return (1..15).map { getMockTestMeta(level, it) }
    }
}
