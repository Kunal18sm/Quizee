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
}
