package com.quizlock.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class QuestionRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_QUESTIONS, Context.MODE_PRIVATE)
    private val gson = Gson()

    private val questionsList = mutableListOf<Question>()

    // Subject-wise cached lists for daily curriculum slicing
    val englishQuestions = mutableListOf<Question>()
    val staticGkQuestions = mutableListOf<Question>()
    val historyQuestions = mutableListOf<Question>()
    val geographyQuestions = mutableListOf<Question>()
    val biologyQuestions = mutableListOf<Question>()
    val polityQuestions = mutableListOf<Question>()
    val economicsQuestions = mutableListOf<Question>()

    companion object {
        private const val PREFS_QUESTIONS = "quiz_questions_storage"
        private const val KEY_CUSTOM_QUESTIONS = "custom_questions_json"

        @Volatile
        private var instance: QuestionRepository? = null

        fun getInstance(context: Context): QuestionRepository {
            return instance ?: synchronized(this) {
                instance ?: QuestionRepository(context.applicationContext).also {
                    it.loadQuestions()
                    instance = it
                }
            }
        }
    }

    private fun loadQuestions() {
        questionsList.clear()
        englishQuestions.clear()
        staticGkQuestions.clear()
        historyQuestions.clear()
        geographyQuestions.clear()
        biologyQuestions.clear()
        polityQuestions.clear()
        economicsQuestions.clear()

        // 1. Load clean, high-yield SSC CGL subject banks from assets
        loadFromAssets()

        // 2. Load any custom questions added by user
        val customJson = prefs.getString(KEY_CUSTOM_QUESTIONS, null)
        if (!customJson.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<Question>>() {}.type
                val customList: List<Question> = gson.fromJson(customJson, type)
                if (customList.isNotEmpty()) {
                    questionsList.addAll(0, customList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun loadSubjectAsset(assetName: String, targetList: MutableList<Question>) {
        try {
            context.assets.open(assetName).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<Question>>() {}.type
                    val list: List<Question> = gson.fromJson(reader, type)
                    targetList.addAll(list)
                    questionsList.addAll(list)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromAssets() {
        loadSubjectAsset("english_questions.json", englishQuestions)
        loadSubjectAsset("static_gk_questions.json", staticGkQuestions)
        loadSubjectAsset("history_questions.json", historyQuestions)
        loadSubjectAsset("geography_questions.json", geographyQuestions)
        loadSubjectAsset("biology_questions.json", biologyQuestions)
        loadSubjectAsset("polity_questions.json", polityQuestions)
        loadSubjectAsset("economics_questions.json", economicsQuestions)

        if (questionsList.isEmpty()) {
            loadHardcodedDefaults()
        }
    }

    private fun loadHardcodedDefaults() {
        val defaults = listOf(
            Question(1, "What is the capital of France?", listOf("Berlin", "Madrid", "Paris", "Rome"), 2, "General"),
            Question(2, "Which planet is known as Red Planet?", listOf("Venus", "Mars", "Jupiter", "Saturn"), 1, "Science"),
            Question(3, "What is 15 × 6?", listOf("80", "90", "85", "95"), 1, "Math"),
            Question(4, "Android language?", listOf("Kotlin", "Swift", "Ruby", "PHP"), 0, "Tech"),
            Question(5, "Continents on Earth?", listOf("5", "6", "7", "8"), 2, "Geography")
        )
        questionsList.addAll(defaults)
        staticGkQuestions.addAll(defaults)
    }

    fun getAllQuestions(): List<Question> {
        if (questionsList.isEmpty()) {
            loadQuestions()
        }
        return ArrayList(questionsList)
    }

    /**
     * Retrieves questions for the active lock session.
     * Delegates to DailyStudyManager to return questions from Today's Bank
     * with smart mistake priority & sequential curriculum.
     */
    fun getRandomQuestions(count: Int = 5): List<Question> {
        return try {
            val dailyQuestions = DailyStudyManager.getInstance(context).getQuizQuestions(count)
            if (dailyQuestions.isNotEmpty()) {
                dailyQuestions
            } else {
                fallbackRandom(count)
            }
        } catch (e: Exception) {
            fallbackRandom(count)
        }
    }

    /**
     * Retrieves practice questions for in-app self testing (10, 20, 30, 40, 50 questions).
     * GUARANTEES that questions are strictly from the starting/foundational chapters
     * and active daily/weekly study pool, NEVER grabbing hard/advanced questions
     * from the far end of the subject banks.
     */
    fun getPracticeTestQuestions(count: Int): List<Question> {
        if (questionsList.isEmpty()) {
            loadQuestions()
        }
        val dailyManager = DailyStudyManager.getInstance(context)
        val todayBank = dailyManager.getTodayBankQuestions()
        val weeklyBank = dailyManager.getWeeklyBankQuestions()

        val activePool = LinkedHashSet<Question>()
        activePool.addAll(todayBank)
        activePool.addAll(weeklyBank)

        // If the user requested count is covered by active pool, sample from active pool
        if (count <= activePool.size && activePool.isNotEmpty()) {
            val mistakeIds = dailyManager.getMistakeIds()
            val seenIds = dailyManager.getSeenIds()

            val mistakes = activePool.filter { it.id in mistakeIds }.shuffled()
            val unseen = activePool.filter { it.id !in seenIds && it.id !in mistakeIds }.shuffled()
            val others = activePool.filter { it.id in seenIds && it.id !in mistakeIds }.shuffled()

            val selected = mutableListOf<Question>()
            selected.addAll(mistakes)
            if (selected.size < count) selected.addAll(unseen.take(count - selected.size))
            if (selected.size < count) selected.addAll(others.take(count - selected.size))
            return selected.shuffled()
        }

        // If count is larger (e.g. 40 or 50 questions requested on early days),
        // we take starting foundational questions from the START (index 0 onwards)
        // of each subject curriculum. NEVER random questions from the deep end!
        val selected = LinkedHashSet<Question>()
        selected.addAll(activePool)

        // Proportional quota from the START of each subject bank
        val engQuota = ((count * 28) / 100) + 4
        val gkQuota = ((count * 20) / 100) + 4
        val histQuota = ((count * 16) / 100) + 3
        val geoQuota = ((count * 10) / 100) + 2
        val bioQuota = ((count * 10) / 100) + 2
        val polQuota = ((count * 10) / 100) + 2
        val ecoQuota = ((count * 6) / 100) + 2

        selected.addAll(englishQuestions.take(engQuota))
        selected.addAll(staticGkQuestions.take(gkQuota))
        selected.addAll(historyQuestions.take(histQuota))
        selected.addAll(geographyQuestions.take(geoQuota))
        selected.addAll(biologyQuestions.take(bioQuota))
        selected.addAll(polityQuestions.take(polQuota))
        selected.addAll(economicsQuestions.take(ecoQuota))

        val list = selected.toList().shuffled()
        return if (list.size >= count) list.take(count) else list
    }

    private fun fallbackRandom(count: Int): List<Question> {
        val all = getAllQuestions()
        if (all.isEmpty()) return emptyList()
        val shuffled = all.shuffled()
        return if (shuffled.size >= count) shuffled.subList(0, count) else shuffled
    }

    @Volatile
    private var questionMapCache: Map<Long, Question>? = null

    fun getQuestionById(id: Long): Question? {
        val map = questionMapCache ?: synchronized(this) {
            questionMapCache ?: questionsList.associateBy { it.id }.also { questionMapCache = it }
        }
        return map[id]
    }

    fun getQuestionsByIds(ids: List<Long>): List<Question> {
        val map = questionMapCache ?: synchronized(this) {
            questionMapCache ?: questionsList.associateBy { it.id }.also { questionMapCache = it }
        }
        return ids.mapNotNull { map[it] }
    }

    fun addQuestion(question: Question) {
        questionMapCache = null
        questionsList.add(0, question)
        saveQuestions()
    }

    fun deleteQuestion(id: Long) {
        questionMapCache = null
        questionsList.removeAll { it.id == id }
        englishQuestions.removeAll { it.id == id }
        staticGkQuestions.removeAll { it.id == id }
        historyQuestions.removeAll { it.id == id }
        geographyQuestions.removeAll { it.id == id }
        biologyQuestions.removeAll { it.id == id }
        polityQuestions.removeAll { it.id == id }
        economicsQuestions.removeAll { it.id == id }
        saveQuestions()
    }

    private fun saveQuestions() {
        val customOnly = questionsList.filter { it.category == "Custom" }
        val json = gson.toJson(customOnly)
        prefs.edit().putString(KEY_CUSTOM_QUESTIONS, json).apply()
    }
}
