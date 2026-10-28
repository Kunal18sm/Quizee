package com.quizlock.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.view.View
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quizlock.app.MainActivity
import com.quizlock.app.R
import com.quizlock.app.data.DailyStudyManager
import com.quizlock.app.data.Question
import com.quizlock.app.data.QuestionRepository

class QuizWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_SELECT_OPTION = "com.quizlock.app.widget.ACTION_SELECT_OPTION"
        const val ACTION_NEXT_QUESTION = "com.quizlock.app.widget.ACTION_NEXT_QUESTION"
        const val ACTION_REFRESH = "com.quizlock.app.widget.ACTION_REFRESH"

        const val EXTRA_OPTION_INDEX = "extra_option_index"

        private const val PREFS_WIDGET = "quiz_widget_storage"
        private const val KEY_PREFIX_Q_ID = "w_qid_"
        private const val KEY_PREFIX_Q_TEXT = "w_qtext_"
        private const val KEY_PREFIX_Q_CAT = "w_qcat_"
        private const val KEY_PREFIX_Q_OPTS = "w_qopts_"
        private const val KEY_PREFIX_Q_CORRECT = "w_qcorrect_"
        private const val KEY_PREFIX_ANSWERED = "w_answered_"
        private const val KEY_PREFIX_SELECTED = "w_selected_"
        private const val KEY_PREFIX_FEEDBACK = "w_feedback_"

        private val OPTION_BUTTON_IDS = intArrayOf(
            R.id.widget_btn_opt1,
            R.id.widget_btn_opt2,
            R.id.widget_btn_opt3,
            R.id.widget_btn_opt4
        )

        private val OPTION_LABELS = arrayOf("A", "B", "C", "D")

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, QuizWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateWidgetView(context, appWidgetManager, widgetId)
            }
        }

        private fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
        }

        fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = getPrefs(context)
            val views = RemoteViews(context.packageName, R.layout.widget_quiz_layout)

            // Setup Launch App intent when tapping Logo or Title
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_img_logo, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_tv_title, launchPendingIntent)

            // Setup Refresh intent
            val refreshIntent = Intent(context, QuizWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 8,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // Setup Next Question intent
            val nextIntent = Intent(context, QuizWidgetProvider::class.java).apply {
                action = ACTION_NEXT_QUESTION
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId * 10 + 9,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            // Load Streak from DailyStudyManager
            try {
                val progress = DailyStudyManager.getInstance(context).getDailyProgress()
                views.setTextViewText(R.id.widget_tv_streak, "🔥 ${progress.streak} Streak")
            } catch (e: Exception) {
                views.setTextViewText(R.id.widget_tv_streak, "🔥 1 Streak")
            }

            // Retrieve or initialize current question
            var questionText = prefs.getString(KEY_PREFIX_Q_TEXT + appWidgetId, null)
            if (questionText.isNullOrEmpty()) {
                // Initialize with fresh question
                val newQ = fetchNextQuestion(context, appWidgetId)
                if (newQ != null) {
                    saveQuestionState(prefs, appWidgetId, newQ)
                    questionText = newQ.question
                }
            }

            val category = prefs.getString(KEY_PREFIX_Q_CAT + appWidgetId, "Quiz")
            val correctIndex = prefs.getInt(KEY_PREFIX_Q_CORRECT + appWidgetId, 0)
            val isAnswered = prefs.getBoolean(KEY_PREFIX_ANSWERED + appWidgetId, false)
            val selectedOption = prefs.getInt(KEY_PREFIX_SELECTED + appWidgetId, -1)
            val feedback = prefs.getString(KEY_PREFIX_FEEDBACK + appWidgetId, "Tap an option to answer")

            val gson = Gson()
            val optionsJson = prefs.getString(KEY_PREFIX_Q_OPTS + appWidgetId, "[]")
            val type = object : TypeToken<List<String>>() {}.type
            val options: List<String> = try {
                gson.fromJson(optionsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            views.setTextViewText(R.id.widget_tv_question, questionText ?: "Tap refresh to load question")
            views.setTextViewText(R.id.widget_tv_category, category)
            views.setTextViewText(R.id.widget_tv_feedback, feedback)

            // Populate option buttons and set their state
            for (i in OPTION_BUTTON_IDS.indices) {
                val btnId = OPTION_BUTTON_IDS[i]
                val optionText = if (i < options.size) options[i] else ""
                val label = OPTION_LABELS.getOrElse(i) { "" }

                views.setTextViewText(btnId, "$label. $optionText")

                if (isAnswered) {
                    // Show correct answer in green
                    if (i == correctIndex) {
                        views.setInt(btnId, "setBackgroundResource", R.drawable.bg_widget_option_correct)
                    } else if (i == selectedOption) {
                        // User's wrong pick in red
                        views.setInt(btnId, "setBackgroundResource", R.drawable.bg_widget_option_wrong)
                    } else {
                        views.setInt(btnId, "setBackgroundResource", R.drawable.bg_widget_option_default)
                    }
                } else {
                    views.setInt(btnId, "setBackgroundResource", R.drawable.bg_widget_option_default)

                    // Set click pending intent for each option
                    val optIntent = Intent(context, QuizWidgetProvider::class.java).apply {
                        action = ACTION_SELECT_OPTION
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(EXTRA_OPTION_INDEX, i)
                    }
                    val optPendingIntent = PendingIntent.getBroadcast(
                        context,
                        appWidgetId * 10 + i,
                        optIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(btnId, optPendingIntent)
                }
            }

            // Next button is visible only after question is answered
            views.setViewVisibility(
                R.id.widget_btn_next,
                if (isAnswered) View.VISIBLE else View.GONE
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun fetchNextQuestion(context: Context, appWidgetId: Int): Question? {
            val currentQid = getPrefs(context).getLong(KEY_PREFIX_Q_ID + appWidgetId, -1L)
            val repo = QuestionRepository.getInstance(context)

            // Try to get from daily study curriculum first
            val pool = try {
                val daily = DailyStudyManager.getInstance(context).getQuizQuestions(5)
                if (daily.isNotEmpty()) daily else repo.getRandomQuestions(5)
            } catch (e: Exception) {
                repo.getRandomQuestions(5)
            }

            if (pool.isEmpty()) return null

            // Pick one that is not the same as current
            val candidate = pool.firstOrNull { it.id != currentQid } ?: pool.first()
            return candidate
        }

        private fun saveQuestionState(
            prefs: SharedPreferences,
            appWidgetId: Int,
            question: Question
        ) {
            val gson = Gson()
            prefs.edit()
                .putLong(KEY_PREFIX_Q_ID + appWidgetId, question.id)
                .putString(KEY_PREFIX_Q_TEXT + appWidgetId, question.question)
                .putString(KEY_PREFIX_Q_CAT + appWidgetId, question.category)
                .putString(KEY_PREFIX_Q_OPTS + appWidgetId, gson.toJson(question.options))
                .putInt(KEY_PREFIX_Q_CORRECT + appWidgetId, question.correctIndex)
                .putBoolean(KEY_PREFIX_ANSWERED + appWidgetId, false)
                .putInt(KEY_PREFIX_SELECTED + appWidgetId, -1)
                .putString(KEY_PREFIX_FEEDBACK + appWidgetId, "Tap an option to answer")
                .apply()
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidgetView(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

        val prefs = getPrefs(context)
        val appWidgetManager = AppWidgetManager.getInstance(context)

        when (intent.action) {
            ACTION_SELECT_OPTION -> {
                val isAnswered = prefs.getBoolean(KEY_PREFIX_ANSWERED + appWidgetId, false)
                if (!isAnswered) {
                    val selectedIndex = intent.getIntExtra(EXTRA_OPTION_INDEX, -1)
                    val correctIndex = prefs.getInt(KEY_PREFIX_Q_CORRECT + appWidgetId, 0)
                    val questionId = prefs.getLong(KEY_PREFIX_Q_ID + appWidgetId, 0L)
                    val isCorrect = (selectedIndex == correctIndex)

                    val feedbackText = if (isCorrect) {
                        "✅ Correct! Well done (+10 XP)"
                    } else {
                        val correctLabel = OPTION_LABELS.getOrElse(correctIndex) { "" }
                        "❌ Wrong! Correct answer: $correctLabel"
                    }

                    // Record in DailyStudyManager for continuous mastery & streak
                    try {
                        DailyStudyManager.getInstance(context).recordAnswer(questionId, isCorrect)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    prefs.edit()
                        .putBoolean(KEY_PREFIX_ANSWERED + appWidgetId, true)
                        .putInt(KEY_PREFIX_SELECTED + appWidgetId, selectedIndex)
                        .putString(KEY_PREFIX_FEEDBACK + appWidgetId, feedbackText)
                        .apply()

                    updateWidgetView(context, appWidgetManager, appWidgetId)
                }
            }

            ACTION_NEXT_QUESTION, ACTION_REFRESH -> {
                val newQ = fetchNextQuestion(context, appWidgetId)
                if (newQ != null) {
                    saveQuestionState(prefs, appWidgetId, newQ)
                }
                updateWidgetView(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = getPrefs(context).edit()
        for (id in appWidgetIds) {
            editor.remove(KEY_PREFIX_Q_ID + id)
            editor.remove(KEY_PREFIX_Q_TEXT + id)
            editor.remove(KEY_PREFIX_Q_CAT + id)
            editor.remove(KEY_PREFIX_Q_OPTS + id)
            editor.remove(KEY_PREFIX_Q_CORRECT + id)
            editor.remove(KEY_PREFIX_ANSWERED + id)
            editor.remove(KEY_PREFIX_SELECTED + id)
            editor.remove(KEY_PREFIX_FEEDBACK + id)
        }
        editor.apply()
    }
}
