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
        
        // 1. Load the comprehensive 2,500+ Static GK questions from assets
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

    private fun loadFromAssets() {
        try {
            context.assets.open("questions.json").use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    val type = object : TypeToken<List<Question>>() {}.type
                    val list: List<Question> = gson.fromJson(reader, type)
                    questionsList.addAll(list)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback hardcoded questions if asset cannot be read
            loadHardcodedDefaults()
        }
    }

    private fun loadHardcodedDefaults() {
        questionsList.add(Question(1, "What is the capital of France?", listOf("Berlin", "Madrid", "Paris", "Rome"), 2, "General"))
        questionsList.add(Question(2, "Which planet is known as Red Planet?", listOf("Venus", "Mars", "Jupiter", "Saturn"), 1, "Science"))
        questionsList.add(Question(3, "What is 15 × 6?", listOf("80", "90", "85", "95"), 1, "Math"))
        questionsList.add(Question(4, "Android language?", listOf("Kotlin", "Swift", "Ruby", "PHP"), 0, "Tech"))
        questionsList.add(Question(5, "Continents on Earth?", listOf("5", "6", "7", "8"), 2, "Geography"))
    }

    fun getAllQuestions(): List<Question> {
        if (questionsList.isEmpty()) {
            loadQuestions()
        }
        return ArrayList(questionsList)
    }

    fun getRandomQuestions(count: Int = 5): List<Question> {
        val all = getAllQuestions()
        if (all.isEmpty()) return emptyList()
        val shuffled = all.shuffled()
        return if (shuffled.size >= count) shuffled.subList(0, count) else shuffled
    }

    fun addQuestion(question: Question) {
        questionsList.add(0, question)
        saveQuestions()
    }

    fun deleteQuestion(id: Long) {
        questionsList.removeAll { it.id == id }
        saveQuestions()
    }

    private fun saveQuestions() {
        val customOnly = questionsList.filter { it.category == "Custom" }
        val json = gson.toJson(customOnly)
        prefs.edit().putString(KEY_CUSTOM_QUESTIONS, json).apply()
    }
}
