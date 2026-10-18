package com.quizlock.app.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DailyProgress(
    val dayNumber: Int,
    val streak: Int,
    val targetSize: Int,
    val seenCount: Int,
    val masteredCount: Int,
    val mistakeCount: Int,
    val isSundayRevision: Boolean = false,
    val weeklyPoolSize: Int = 0
) {
    val isFullyMastered: Boolean
        get() = masteredCount >= targetSize && targetSize > 0

    val progressPercent: Int
        get() = if (targetSize > 0) ((masteredCount.toFloat() / targetSize) * 100).toInt().coerceIn(0, 100) else 0
}

data class SubjectQuota(
    val english: Int,
    val staticGk: Int,
    val history: Int,
    val geography: Int,
    val biology: Int,
    val polity: Int,
    val economics: Int
) {
    val total: Int
        get() = english + staticGk + history + geography + biology + polity + economics
}

class DailyStudyManager private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "quiz_daily_study_prefs"

        private const val KEY_TARGET_SIZE = "daily_target_size" // 30 or 20
        private const val KEY_LAST_DATE = "daily_last_date"
        private const val KEY_DAY_NUMBER = "daily_day_number"
        private const val KEY_STREAK = "daily_streak"

        // Subject sequential pointers
        private const val KEY_PTR_ENGLISH = "ptr_english"
        private const val KEY_PTR_STATIC_GK = "ptr_static_gk"
        private const val KEY_PTR_HISTORY = "ptr_history"
        private const val KEY_PTR_GEOGRAPHY = "ptr_geography"
        private const val KEY_PTR_BIOLOGY = "ptr_biology"
        private const val KEY_PTR_POLITY = "ptr_polity"
        private const val KEY_PTR_ECONOMICS = "ptr_economics"

        // Daily runtime state (JSON of List<Long>)
        private const val KEY_TODAY_BANK_IDS = "today_bank_ids"
        private const val KEY_TODAY_SEEN_IDS = "today_seen_ids"
        private const val KEY_TODAY_MASTERED_IDS = "today_mastered_ids"
        private const val KEY_TODAY_MISTAKE_IDS = "today_mistake_ids"

        // Weekly Sunday revision state
        private const val KEY_WEEKLY_BANK_IDS = "weekly_accumulated_bank_ids"
        private const val KEY_CURRENT_WEEK_OF_YEAR = "current_week_of_year"

        const val DEFAULT_TARGET_SIZE = 30

        @Volatile
        private var instance: DailyStudyManager? = null

        fun getInstance(context: Context): DailyStudyManager {
            return instance ?: synchronized(this) {
                instance ?: DailyStudyManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun isTodaySunday(): Boolean {
        return Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
    }

    fun getTargetSize(): Int {
        return prefs.getInt(KEY_TARGET_SIZE, DEFAULT_TARGET_SIZE)
    }

    fun setTargetSize(size: Int) {
        val validSize = if (size == 20) 20 else 30
        if (validSize != getTargetSize()) {
            prefs.edit().putInt(KEY_TARGET_SIZE, validSize).apply()
            // Regenerate today's bank with new quota
            if (!isSundayRevisionActive()) {
                generateRegularDayBank()
            }
        }
    }

    fun getQuotaForTarget(target: Int): SubjectQuota {
        return if (target == 20) {
            SubjectQuota(
                english = 5,
                staticGk = 4,
                history = 3,
                geography = 2,
                biology = 2,
                polity = 2,
                economics = 2
            )
        } else {
            SubjectQuota(
                english = 8,
                staticGk = 6,
                history = 5,
                geography = 3,
                biology = 3,
                polity = 3,
                economics = 2
            )
        }
    }

    fun isSundayRevisionActive(): Boolean {
        val isSunday = isTodaySunday()
        val dayNum = prefs.getInt(KEY_DAY_NUMBER, 1)
        val weeklyIds = getSavedIdList(KEY_WEEKLY_BANK_IDS)
        return isSunday && dayNum > 1 && weeklyIds.isNotEmpty()
    }

    private fun getCurrentDateString(): String = dateFormat.format(Date())

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -1)
        return dateFormat.format(cal.time)
    }

    @Synchronized
    fun ensureDailyBankReady() {
        val todayStr = getCurrentDateString()
        val lastDateStr = prefs.getString(KEY_LAST_DATE, null)
        val currentWeek = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR)
        val savedWeek = prefs.getInt(KEY_CURRENT_WEEK_OF_YEAR, -1)
        val dayNum = prefs.getInt(KEY_DAY_NUMBER, 1)

        // Fix migration: if Day 1 had an accidental 60-question pool from previous build, clean it up!
        if (dayNum <= 1) {
            val currentBank = getSavedIdList(KEY_TODAY_BANK_IDS)
            if (currentBank.size > 30) {
                prefs.edit().putString(KEY_WEEKLY_BANK_IDS, gson.toJson(emptyList<Long>())).apply()
                generateTodayBank()
            }
        }

        if (lastDateStr == null) {
            // First time initialization
            prefs.edit()
                .putString(KEY_LAST_DATE, todayStr)
                .putInt(KEY_DAY_NUMBER, 1)
                .putInt(KEY_STREAK, 1)
                .putInt(KEY_CURRENT_WEEK_OF_YEAR, currentWeek)
                .putString(KEY_WEEKLY_BANK_IDS, gson.toJson(emptyList<Long>()))
                .apply()
            generateTodayBank()
        } else if (lastDateStr != todayStr) {
            // Date changed (Midnight transition!)
            val yesterdayStr = getYesterdayDateString()
            val currentStreak = prefs.getInt(KEY_STREAK, 1)
            val newStreak = if (lastDateStr == yesterdayStr) currentStreak + 1 else 1
            val currentDayNum = prefs.getInt(KEY_DAY_NUMBER, 1)

            // On Sunday revision, we DO NOT advance pointers! Pointers stay frozen for Sunday weekly revision.
            // On regular study days, advance pointers by quota.
            if (!isSundayRevisionActive()) {
                advancePointersByQuota()
            }

            // If it's Monday or a new calendar week, start fresh weekly accumulation
            if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY || savedWeek != currentWeek) {
                if (!isTodaySunday()) {
                    prefs.edit()
                        .putString(KEY_WEEKLY_BANK_IDS, gson.toJson(emptyList<Long>()))
                        .putInt(KEY_CURRENT_WEEK_OF_YEAR, currentWeek)
                        .apply()
                }
            }

            prefs.edit()
                .putString(KEY_LAST_DATE, todayStr)
                .putInt(KEY_DAY_NUMBER, currentDayNum + 1)
                .putInt(KEY_STREAK, newStreak)
                .apply()

            generateTodayBank()
        } else {
            // Same day: verify today's bank is not empty
            val bankIds = getSavedIdList(KEY_TODAY_BANK_IDS)
            if (bankIds.isEmpty()) {
                generateTodayBank()
            }
        }
    }

    @Synchronized
    fun advanceToNextDayManually() {
        // User manually wants next bank
        advancePointersByQuota()
        val currentDayNum = prefs.getInt(KEY_DAY_NUMBER, 1)
        val currentStreak = prefs.getInt(KEY_STREAK, 1)

        prefs.edit()
            .putString(KEY_LAST_DATE, getCurrentDateString())
            .putInt(KEY_DAY_NUMBER, currentDayNum + 1)
            .putInt(KEY_STREAK, currentStreak + 1)
            .apply()

        generateRegularDayBank()
    }

    private fun advancePointersByQuota() {
        val quota = getQuotaForTarget(getTargetSize())
        val repo = QuestionRepository.getInstance(context)

        val totalEng = repo.englishQuestions.size.coerceAtLeast(1)
        val totalGk = repo.staticGkQuestions.size.coerceAtLeast(1)
        val totalHist = repo.historyQuestions.size.coerceAtLeast(1)
        val totalGeo = repo.geographyQuestions.size.coerceAtLeast(1)
        val totalBio = repo.biologyQuestions.size.coerceAtLeast(1)
        val totalPol = repo.polityQuestions.size.coerceAtLeast(1)
        val totalEco = repo.economicsQuestions.size.coerceAtLeast(1)

        val newEng = (prefs.getInt(KEY_PTR_ENGLISH, 0) + quota.english) % totalEng
        val newGk = (prefs.getInt(KEY_PTR_STATIC_GK, 0) + quota.staticGk) % totalGk
        val newHist = (prefs.getInt(KEY_PTR_HISTORY, 0) + quota.history) % totalHist
        val newGeo = (prefs.getInt(KEY_PTR_GEOGRAPHY, 0) + quota.geography) % totalGeo
        val newBio = (prefs.getInt(KEY_PTR_BIOLOGY, 0) + quota.biology) % totalBio
        val newPol = (prefs.getInt(KEY_PTR_POLITY, 0) + quota.polity) % totalPol
        val newEco = (prefs.getInt(KEY_PTR_ECONOMICS, 0) + quota.economics) % totalEco

        prefs.edit()
            .putInt(KEY_PTR_ENGLISH, newEng)
            .putInt(KEY_PTR_STATIC_GK, newGk)
            .putInt(KEY_PTR_HISTORY, newHist)
            .putInt(KEY_PTR_GEOGRAPHY, newGeo)
            .putInt(KEY_PTR_BIOLOGY, newBio)
            .putInt(KEY_PTR_POLITY, newPol)
            .putInt(KEY_PTR_ECONOMICS, newEco)
            .apply()
    }

    private fun <T> sliceSequential(list: List<T>, start: Int, count: Int): List<T> {
        if (list.isEmpty() || count <= 0) return emptyList()
        val result = mutableListOf<T>()
        var idx = start % list.size
        for (i in 0 until count) {
            result.add(list[idx])
            idx = (idx + 1) % list.size
        }
        return result
    }

    private fun generateTodayBank() {
        if (isSundayRevisionActive()) {
            generateSundayRevisionBank()
        } else {
            generateRegularDayBank()
        }
    }

    private fun generateSundayRevisionBank() {
        val weeklyIds = getSavedIdList(KEY_WEEKLY_BANK_IDS)
        prefs.edit()
            .putString(KEY_TODAY_BANK_IDS, gson.toJson(weeklyIds))
            .putString(KEY_TODAY_SEEN_IDS, gson.toJson(emptyList<Long>()))
            .putString(KEY_TODAY_MASTERED_IDS, gson.toJson(emptyList<Long>()))
            .putString(KEY_TODAY_MISTAKE_IDS, gson.toJson(emptyList<Long>()))
            .apply()
    }

    private fun generateRegularDayBank() {
        val repo = QuestionRepository.getInstance(context)
        val quota = getQuotaForTarget(getTargetSize())

        val engPtr = prefs.getInt(KEY_PTR_ENGLISH, 0)
        val gkPtr = prefs.getInt(KEY_PTR_STATIC_GK, 0)
        val histPtr = prefs.getInt(KEY_PTR_HISTORY, 0)
        val geoPtr = prefs.getInt(KEY_PTR_GEOGRAPHY, 0)
        val bioPtr = prefs.getInt(KEY_PTR_BIOLOGY, 0)
        val polPtr = prefs.getInt(KEY_PTR_POLITY, 0)
        val ecoPtr = prefs.getInt(KEY_PTR_ECONOMICS, 0)

        val selected = mutableListOf<Question>()
        selected.addAll(sliceSequential(repo.englishQuestions, engPtr, quota.english))
        selected.addAll(sliceSequential(repo.staticGkQuestions, gkPtr, quota.staticGk))
        selected.addAll(sliceSequential(repo.historyQuestions, histPtr, quota.history))
        selected.addAll(sliceSequential(repo.geographyQuestions, geoPtr, quota.geography))
        selected.addAll(sliceSequential(repo.biologyQuestions, bioPtr, quota.biology))
        selected.addAll(sliceSequential(repo.polityQuestions, polPtr, quota.polity))
        selected.addAll(sliceSequential(repo.economicsQuestions, ecoPtr, quota.economics))

        val bankIds = selected.map { it.id }

        // Accumulate into weekly bank for Sunday's revision
        val currentWeekly = getSavedIdList(KEY_WEEKLY_BANK_IDS).toMutableSet()
        currentWeekly.addAll(bankIds)
        prefs.edit().putString(KEY_WEEKLY_BANK_IDS, gson.toJson(currentWeekly.toList())).apply()

        prefs.edit()
            .putString(KEY_TODAY_BANK_IDS, gson.toJson(bankIds))
            .putString(KEY_TODAY_SEEN_IDS, gson.toJson(emptyList<Long>()))
            .putString(KEY_TODAY_MASTERED_IDS, gson.toJson(emptyList<Long>()))
            .putString(KEY_TODAY_MISTAKE_IDS, gson.toJson(emptyList<Long>()))
            .apply()
    }

    fun getTodayBankQuestions(): List<Question> {
        ensureDailyBankReady()
        val bankIds = getSavedIdList(KEY_TODAY_BANK_IDS).toSet()
        val repo = QuestionRepository.getInstance(context)
        val all = repo.getAllQuestions()
        return all.filter { it.id in bankIds }
    }

    /**
     * Smart Quiz Sampling (5 questions from Today's Bank):
     * 1. Priority 1: Mistakes made earlier today (so user corrects them!)
     * 2. Priority 2: Unseen questions from today's bank
     * 3. Priority 3: Random reinforcement from today's bank
     */
    @Synchronized
    fun getQuizQuestions(count: Int = 5): List<Question> {
        ensureDailyBankReady()
        val todayBank = getTodayBankQuestions()
        if (todayBank.isEmpty()) {
            return QuestionRepository.getInstance(context).getAllQuestions().shuffled().take(count)
        }

        val mistakeIds = getSavedIdList(KEY_TODAY_MISTAKE_IDS).toSet()
        val seenIds = getSavedIdList(KEY_TODAY_SEEN_IDS).toSet()

        val mistakes = todayBank.filter { it.id in mistakeIds }.shuffled()
        val unseen = todayBank.filter { it.id !in seenIds && it.id !in mistakeIds }.shuffled()
        val others = todayBank.filter { it.id in seenIds && it.id !in mistakeIds }.shuffled()

        val selected = mutableListOf<Question>()

        // 1. Add mistake questions first (up to 2 per quiz so not all are mistakes)
        val mistakeLimit = 2.coerceAtMost(count)
        selected.addAll(mistakes.take(mistakeLimit))

        // 2. Add unseen questions
        val remainingNeeded1 = count - selected.size
        if (remainingNeeded1 > 0) {
            selected.addAll(unseen.take(remainingNeeded1))
        }

        // 3. Add reinforcement / others if needed
        val remainingNeeded2 = count - selected.size
        if (remainingNeeded2 > 0) {
            selected.addAll(others.take(remainingNeeded2))
        }

        // 4. If still not enough (e.g. edge cases), fill from any remaining in bank
        if (selected.size < count) {
            val remainingInBank = todayBank.filter { it !in selected }.shuffled()
            selected.addAll(remainingInBank.take(count - selected.size))
        }

        // Mark selected as seen
        val updatedSeen = getSavedIdList(KEY_TODAY_SEEN_IDS).toMutableSet()
        selected.forEach { updatedSeen.add(it.id) }
        prefs.edit().putString(KEY_TODAY_SEEN_IDS, gson.toJson(updatedSeen.toList())).apply()

        return selected.shuffled()
    }

    @Synchronized
    fun recordAnswer(questionId: Long, isCorrect: Boolean) {
        val mastered = getSavedIdList(KEY_TODAY_MASTERED_IDS).toMutableSet()
        val mistakes = getSavedIdList(KEY_TODAY_MISTAKE_IDS).toMutableSet()

        if (isCorrect) {
            mastered.add(questionId)
            mistakes.remove(questionId) // Cleared mistake!
        } else {
            mistakes.add(questionId) // Needs re-attempt in next quiz
        }

        prefs.edit()
            .putString(KEY_TODAY_MASTERED_IDS, gson.toJson(mastered.toList()))
            .putString(KEY_TODAY_MISTAKE_IDS, gson.toJson(mistakes.toList()))
            .apply()
    }

    fun getDailyProgress(): DailyProgress {
        ensureDailyBankReady()
        val dayNum = prefs.getInt(KEY_DAY_NUMBER, 1)
        val streak = prefs.getInt(KEY_STREAK, 1)
        val isSundayRev = isSundayRevisionActive()
        val todayBankIds = getSavedIdList(KEY_TODAY_BANK_IDS)
        val targetSize = if (isSundayRev) todayBankIds.size.coerceAtLeast(1) else getTargetSize()

        val seenCount = getSavedIdList(KEY_TODAY_SEEN_IDS).size
        val masteredCount = getSavedIdList(KEY_TODAY_MASTERED_IDS).size
        val mistakeCount = getSavedIdList(KEY_TODAY_MISTAKE_IDS).size

        return DailyProgress(
            dayNumber = dayNum,
            streak = streak,
            targetSize = targetSize,
            seenCount = seenCount.coerceAtMost(targetSize),
            masteredCount = masteredCount.coerceAtMost(targetSize),
            mistakeCount = mistakeCount,
            isSundayRevision = isSundayRev,
            weeklyPoolSize = todayBankIds.size
        )
    }

    fun getMasteredIds(): Set<Long> = getSavedIdList(KEY_TODAY_MASTERED_IDS).toSet()
    fun getMistakeIds(): Set<Long> = getSavedIdList(KEY_TODAY_MISTAKE_IDS).toSet()
    fun getSeenIds(): Set<Long> = getSavedIdList(KEY_TODAY_SEEN_IDS).toSet()

    private fun getSavedIdList(key: String): List<Long> {
        val json = prefs.getString(key, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Long>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
