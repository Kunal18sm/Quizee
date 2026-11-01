package com.quizlock.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.quizlock.app.R
import com.quizlock.app.data.DailyStudyManager
import com.quizlock.app.data.PracticeRepository
import com.quizlock.app.data.Question
import com.quizlock.app.data.QuestionRepository
import com.quizlock.app.ui.CustomPracticeActivity
import com.quizlock.app.ui.CustomWidgetConfigureActivity

class CustomTopicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_SELECT_OPTION = "com.quizlock.app.widget.custom.ACTION_SELECT_OPTION"
        const val ACTION_NEXT_QUESTION = "com.quizlock.app.widget.custom.ACTION_NEXT_QUESTION"
        const val ACTION_REFRESH = "com.quizlock.app.widget.custom.ACTION_REFRESH"

        const val EXTRA_OPTION_INDEX = "extra_option_index"

        private const val PREFS_CUSTOM_WIDGET = "custom_topic_widget_storage"
        private const val KEY_SUBJECT = "w_subj_"
        private const val KEY_TOPIC = "w_topic_"
        private const val KEY_SET_NUM = "w_setnum_"
        private const val KEY_SET_KEY = "w_setkey_"
        private const val KEY_Q_IDS = "w_qids_"
        private const val KEY_CURRENT_IDX = "w_curidx_"
        private const val KEY_Q_ID = "w_qid_"
        private const val KEY_Q_TEXT = "w_qtext_"
        private const val KEY_Q_OPTS = "w_qopts_"
        private const val KEY_Q_CORRECT = "w_qcorrect_"
        private const val KEY_ANSWERED = "w_answered_"
        private const val KEY_SELECTED = "w_selected_"
        private const val KEY_FEEDBACK = "w_feedback_"

        // Global default keys when adding new widgets
        private const val KEY_DEF_SUBJECT = "def_subject"
        private const val KEY_DEF_TOPIC = "def_topic"
        private const val KEY_DEF_SET_NUM = "def_set_num"
        private const val KEY_DEF_SET_KEY = "def_set_key"
        private const val KEY_DEF_Q_IDS = "def_q_ids"

        private val OPTION_BUTTON_IDS = intArrayOf(
            R.id.widget_btn_opt1,
            R.id.widget_btn_opt2,
            R.id.widget_btn_opt3,
            R.id.widget_btn_opt4
        )

        private val OPTION_LABELS = arrayOf("A", "B", "C", "D")

        fun getPrefs(context: Context): SharedPreferences {
            return context.getSharedPreferences(PREFS_CUSTOM_WIDGET, Context.MODE_PRIVATE)
        }

        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, CustomTopicWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateWidgetView(context, appWidgetManager, widgetId)
            }
        }

        fun saveDefaultSelection(
            context: Context,
            subject: String,
            topic: String,
            setNum: Int,
            setKey: String,
            questionIds: List<Long>
        ) {
            val gson = Gson()
            getPrefs(context).edit()
                .putString(KEY_DEF_SUBJECT, subject)
                .putString(KEY_DEF_TOPIC, topic)
                .putInt(KEY_DEF_SET_NUM, setNum)
                .putString(KEY_DEF_SET_KEY, setKey)
                .putString(KEY_DEF_Q_IDS, gson.toJson(questionIds))
                .apply()
        }

        fun configureWidget(
            context: Context,
            appWidgetId: Int,
            subject: String,
            topic: String,
            setNum: Int,
            setKey: String,
            questionIds: List<Long>
        ) {
            val prefs = getPrefs(context)
            val gson = Gson()

            // Save configuration
            prefs.edit()
                .putString(KEY_SUBJECT + appWidgetId, subject)
                .putString(KEY_TOPIC + appWidgetId, topic)
                .putInt(KEY_SET_NUM + appWidgetId, setNum)
                .putString(KEY_SET_KEY + appWidgetId, setKey)
                .putString(KEY_Q_IDS + appWidgetId, gson.toJson(questionIds))
                .putInt(KEY_CURRENT_IDX + appWidgetId, 0)
                .apply()

            // Also keep as default for quick additions
            saveDefaultSelection(context, subject, topic, setNum, setKey, questionIds)

            // Immediately load first question of this set
            if (questionIds.isNotEmpty()) {
                val qRepo = QuestionRepository.getInstance(context)
                val q = qRepo.getQuestionById(questionIds[0])
                if (q != null) {
                    saveQuestionState(prefs, appWidgetId, q, 0)
                }
            }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            updateWidgetView(context, appWidgetManager, appWidgetId)
        }

        fun getWidgetConfiguration(context: Context, appWidgetId: Int): Triple<String, String, Int> {
            val prefs = getPrefs(context)
            val subject = prefs.getString(KEY_SUBJECT + appWidgetId, null) ?: "History"
            val topic = prefs.getString(KEY_TOPIC + appWidgetId, null) ?: "Ancient History"
            val setNum = prefs.getInt(KEY_SET_NUM + appWidgetId, 1)
            return Triple(subject, topic, setNum)
        }

        fun updateWidgetView(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs = getPrefs(context)
            val optionsBundle = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minHeight = optionsBundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
            val maxHeight = optionsBundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 0)
            val isExpanded = minHeight >= 260 || maxHeight >= 320
            val layoutId = if (isExpanded) R.layout.widget_custom_topic_layout_expanded else R.layout.widget_custom_topic_layout
            val views = RemoteViews(context.packageName, layoutId)

            // Ensure widget is configured with something valid
            ensureConfigured(context, appWidgetId, prefs)

            val subject = prefs.getString(KEY_SUBJECT + appWidgetId, "History") ?: "History"
            val topic = prefs.getString(KEY_TOPIC + appWidgetId, "Ancient History") ?: "Ancient History"
            val setNum = prefs.getInt(KEY_SET_NUM + appWidgetId, 1)
            val currentIdx = prefs.getInt(KEY_CURRENT_IDX + appWidgetId, 0)

            val questionIds = getStoredQuestionIds(prefs, appWidgetId)
            val totalQuestions = questionIds.size.coerceAtLeast(1)
            val displayQuestionNum = (currentIdx + 1).coerceAtMost(totalQuestions)

            // Header titles and badges
            views.setTextViewText(R.id.widget_tv_badge, "$subject • Set $setNum")
            views.setTextViewText(R.id.widget_tv_counter, "Q $displayQuestionNum/$totalQuestions")
            views.setTextViewText(R.id.widget_tv_topic, "Topic: $topic")

            // Tap Logo or Title to open CustomPracticeActivity in app
            val launchIntent = Intent(context, CustomPracticeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(CustomPracticeActivity.EXTRA_FILTER_SUBJECT, subject)
            }
            val launchPendingIntent = PendingIntent.getActivity(
                context,
                20000 + appWidgetId,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_img_logo, launchPendingIntent)
            views.setOnClickPendingIntent(R.id.widget_tv_title, launchPendingIntent)

            // Tap ⚙️ Settings / Configure Button to change Subject / Topic / Set
            val configIntent = Intent(context, CustomWidgetConfigureActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val configPendingIntent = PendingIntent.getActivity(
                context,
                30000 + appWidgetId,
                configIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_config, configPendingIntent)

            // Setup Refresh intent
            val refreshIntent = Intent(context, CustomTopicWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
                data = Uri.parse("quizee://customwidget/$appWidgetId/refresh")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                40000 + appWidgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // Setup Next Question intent
            val nextIntent = Intent(context, CustomTopicWidgetProvider::class.java).apply {
                action = ACTION_NEXT_QUESTION
                data = Uri.parse("quizee://customwidget/$appWidgetId/next")
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val nextPendingIntent = PendingIntent.getBroadcast(
                context,
                50000 + appWidgetId,
                nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_next, nextPendingIntent)

            // Retrieve or initialize question
            var questionText = prefs.getString(KEY_Q_TEXT + appWidgetId, null)
            if (questionText.isNullOrEmpty()) {
                val q = loadQuestionAt(context, appWidgetId, currentIdx)
                if (q != null) {
                    saveQuestionState(prefs, appWidgetId, q, currentIdx)
                    questionText = q.question
                }
            }

            val correctIndex = prefs.getInt(KEY_Q_CORRECT + appWidgetId, 0)
            val isAnswered = prefs.getBoolean(KEY_ANSWERED + appWidgetId, false)
            val selectedOption = prefs.getInt(KEY_SELECTED + appWidgetId, -1)
            val feedback = prefs.getString(KEY_FEEDBACK + appWidgetId, "Tap an option to answer")

            val gson = Gson()
            val optionsJson = prefs.getString(KEY_Q_OPTS + appWidgetId, "[]")
            val type = object : TypeToken<List<String>>() {}.type
            val options: List<String> = try {
                gson.fromJson(optionsJson, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }

            views.setTextViewText(R.id.widget_tv_question, questionText ?: "Tap ⚙️ to select topic & set")
            views.setTextViewText(R.id.widget_tv_feedback, feedback)

            // Populate option buttons
            for (i in OPTION_BUTTON_IDS.indices) {
                val btnId = OPTION_BUTTON_IDS[i]
                if (i >= options.size) {
                    views.setViewVisibility(btnId, View.GONE)
                    continue
                }
                views.setViewVisibility(btnId, View.VISIBLE)

                val optionText = options[i]
                val label = OPTION_LABELS.getOrElse(i) { "" }
                views.setTextViewText(btnId, "$label. $optionText")

                if (isAnswered) {
                    if (i == correctIndex) {
                        views.setInt(btnId, "setBackgroundResource", R.drawable.bg_widget_option_correct)
                    } else if (i == selectedOption) {
                        views.setInt(btnId, "setBackgroundResource", R.drawable.bg_widget_option_wrong)
                    } else {
                        views.setInt(btnId, "setBackgroundResource", R.drawable.bg_widget_option_default)
                    }
                    views.setOnClickPendingIntent(btnId, null)
                } else {
                    views.setInt(btnId, "setBackgroundResource", R.drawable.bg_widget_option_default)

                    val optIntent = Intent(context, CustomTopicWidgetProvider::class.java).apply {
                        action = ACTION_SELECT_OPTION
                        data = Uri.parse("quizee://customwidget/$appWidgetId/opt/$i")
                        putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                        putExtra(EXTRA_OPTION_INDEX, i)
                    }
                    val optPendingIntent = PendingIntent.getBroadcast(
                        context,
                        60000 + (appWidgetId * 10) + i,
                        optIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(btnId, optPendingIntent)
                }
            }

            // Next button visible only after question is answered
            views.setViewVisibility(
                R.id.widget_btn_next,
                if (isAnswered) View.VISIBLE else View.GONE
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun ensureConfigured(
            context: Context,
            appWidgetId: Int,
            prefs: SharedPreferences
        ) {
            val storedSubj = prefs.getString(KEY_SUBJECT + appWidgetId, null)
            if (!storedSubj.isNullOrEmpty()) return

            // Check if there are default preferences set
            val defSubj = prefs.getString(KEY_DEF_SUBJECT, null)
            if (!defSubj.isNullOrEmpty()) {
                val defTopic = prefs.getString(KEY_DEF_TOPIC, "") ?: ""
                val defSetNum = prefs.getInt(KEY_DEF_SET_NUM, 1)
                val defSetKey = prefs.getString(KEY_DEF_SET_KEY, "") ?: ""
                val defQIdsJson = prefs.getString(KEY_DEF_Q_IDS, "[]")
                val type = object : TypeToken<List<Long>>() {}.type
                val qIds: List<Long> = try {
                    Gson().fromJson(defQIdsJson, type) ?: emptyList()
                } catch (e: Exception) {
                    emptyList()
                }

                if (qIds.isNotEmpty()) {
                    configureWidget(context, appWidgetId, defSubj, defTopic, defSetNum, defSetKey, qIds)
                    return
                }
            }

            // Fallback to first available curriculum subject -> topic -> set
            try {
                val curriculum = PracticeRepository.getInstance(context).getCurriculum()
                if (curriculum.isNotEmpty()) {
                    val firstSub = curriculum[0]
                    if (firstSub.topics.isNotEmpty()) {
                        val firstTopic = firstSub.topics[0]
                        if (firstTopic.sets.isNotEmpty()) {
                            val firstSet = firstTopic.sets[0]
                            configureWidget(
                                context,
                                appWidgetId,
                                firstSub.name,
                                firstTopic.displayName,
                                firstSet.setNumber,
                                firstSet.setKey,
                                firstSet.questionIds
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        private fun getStoredQuestionIds(prefs: SharedPreferences, appWidgetId: Int): List<Long> {
            val json = prefs.getString(KEY_Q_IDS + appWidgetId, "[]")
            val type = object : TypeToken<List<Long>>() {}.type
            return try {
                Gson().fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        private fun loadQuestionAt(context: Context, appWidgetId: Int, index: Int): Question? {
            val prefs = getPrefs(context)
            val ids = getStoredQuestionIds(prefs, appWidgetId)
            if (ids.isEmpty()) return null
            val safeIdx = index.coerceIn(0, ids.size - 1)
            val targetId = ids[safeIdx]
            return QuestionRepository.getInstance(context).getQuestionById(targetId)
        }

        private fun saveQuestionState(
            prefs: SharedPreferences,
            appWidgetId: Int,
            question: Question,
            index: Int
        ) {
            val gson = Gson()
            prefs.edit()
                .putInt(KEY_CURRENT_IDX + appWidgetId, index)
                .putLong(KEY_Q_ID + appWidgetId, question.id)
                .putString(KEY_Q_TEXT + appWidgetId, question.question)
                .putString(KEY_Q_OPTS + appWidgetId, gson.toJson(question.options))
                .putInt(KEY_Q_CORRECT + appWidgetId, question.correctIndex)
                .putBoolean(KEY_ANSWERED + appWidgetId, false)
                .putInt(KEY_SELECTED + appWidgetId, -1)
                .putString(KEY_FEEDBACK + appWidgetId, "Tap an option to answer")
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle
    ) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateWidgetView(context, appWidgetManager, appWidgetId)
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
                val isAnswered = prefs.getBoolean(KEY_ANSWERED + appWidgetId, false)
                if (!isAnswered) {
                    val selectedIndex = intent.getIntExtra(EXTRA_OPTION_INDEX, -1)
                    val correctIndex = prefs.getInt(KEY_Q_CORRECT + appWidgetId, 0)
                    val questionId = prefs.getLong(KEY_Q_ID + appWidgetId, 0L)
                    val isCorrect = (selectedIndex == correctIndex)

                    val feedbackText = if (isCorrect) {
                        "✅ Correct! Well done (+10 XP)"
                    } else {
                        val correctLabel = OPTION_LABELS.getOrElse(correctIndex) { "" }
                        "❌ Wrong! Correct answer: $correctLabel"
                    }

                    // Record answer in DailyStudyManager for continuous mastery & streak
                    try {
                        DailyStudyManager.getInstance(context).recordAnswer(questionId, isCorrect)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    prefs.edit()
                        .putBoolean(KEY_ANSWERED + appWidgetId, true)
                        .putInt(KEY_SELECTED + appWidgetId, selectedIndex)
                        .putString(KEY_FEEDBACK + appWidgetId, feedbackText)
                        .apply()

                    updateWidgetView(context, appWidgetManager, appWidgetId)
                }
            }

            ACTION_NEXT_QUESTION -> {
                val questionIds = getStoredQuestionIds(prefs, appWidgetId)
                if (questionIds.isNotEmpty()) {
                    val currentIdx = prefs.getInt(KEY_CURRENT_IDX + appWidgetId, 0)
                    val nextIdx = (currentIdx + 1) % questionIds.size
                    val newQ = loadQuestionAt(context, appWidgetId, nextIdx)
                    if (newQ != null) {
                        saveQuestionState(prefs, appWidgetId, newQ, nextIdx)
                    }
                }
                updateWidgetView(context, appWidgetManager, appWidgetId)
            }

            ACTION_REFRESH -> {
                val questionIds = getStoredQuestionIds(prefs, appWidgetId)
                if (questionIds.isNotEmpty()) {
                    // Pick a random index or next
                    val currentIdx = prefs.getInt(KEY_CURRENT_IDX + appWidgetId, 0)
                    val nextIdx = (currentIdx + (1..questionIds.size - 1).random().coerceAtLeast(1)) % questionIds.size
                    val newQ = loadQuestionAt(context, appWidgetId, nextIdx)
                    if (newQ != null) {
                        saveQuestionState(prefs, appWidgetId, newQ, nextIdx)
                    }
                }
                updateWidgetView(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val editor = getPrefs(context).edit()
        for (id in appWidgetIds) {
            editor.remove(KEY_SUBJECT + id)
            editor.remove(KEY_TOPIC + id)
            editor.remove(KEY_SET_NUM + id)
            editor.remove(KEY_SET_KEY + id)
            editor.remove(KEY_Q_IDS + id)
            editor.remove(KEY_CURRENT_IDX + id)
            editor.remove(KEY_Q_ID + id)
            editor.remove(KEY_Q_TEXT + id)
            editor.remove(KEY_Q_OPTS + id)
            editor.remove(KEY_Q_CORRECT + id)
            editor.remove(KEY_ANSWERED + id)
            editor.remove(KEY_SELECTED + id)
            editor.remove(KEY_FEEDBACK + id)
        }
        editor.apply()
    }
}
