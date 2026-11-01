package com.quizlock.app.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.quizlock.app.R
import com.quizlock.app.data.PracticeRepository
import com.quizlock.app.data.PracticeSet
import com.quizlock.app.data.PracticeSubject
import com.quizlock.app.data.PracticeTopic
import com.quizlock.app.databinding.ActivityCustomWidgetConfigureBinding
import com.quizlock.app.widget.CustomTopicWidgetProvider

class CustomWidgetConfigureActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomWidgetConfigureBinding
    private lateinit var practiceRepo: PracticeRepository

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var allSubjects: List<PracticeSubject> = emptyList()
    private var selectedSubject: PracticeSubject? = null
    private var selectedTopic: PracticeTopic? = null
    private var selectedSet: PracticeSet? = null

    private val subjectChipMap = mutableMapOf<MaterialButton, String>()

    companion object {
        const val EXTRA_PIN_MODE = "extra_pin_mode"

        fun startForPinning(context: Context) {
            val intent = Intent(context, CustomWidgetConfigureActivity::class.java).apply {
                putExtra(EXTRA_PIN_MODE, true)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If cancelled, return RESULT_CANCELED as required by AppWidget standard
        setResult(Activity.RESULT_CANCELED)

        binding = ActivityCustomWidgetConfigureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        practiceRepo = PracticeRepository.getInstance(this)

        // Read appWidgetId from Intent
        appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )

        val isPinMode = intent.getBooleanExtra(EXTRA_PIN_MODE, false) ||
                (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID)

        setupToolbar(isPinMode)
        setupSubjectChips()
        loadData()
        setupListeners(isPinMode)
    }

    private fun setupToolbar(isPinMode: Boolean) {
        binding.toolbar.title = if (isPinMode) {
            "Add Custom Widget to Home"
        } else {
            "Configure Widget Set"
        }
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupSubjectChips() {
        subjectChipMap[binding.chipSubEnglish] = "English"
        subjectChipMap[binding.chipSubStaticGk] = "Static GK"
        subjectChipMap[binding.chipSubHistory] = "History"
        subjectChipMap[binding.chipSubGeography] = "Geography"
        subjectChipMap[binding.chipSubBiology] = "Biology"
        subjectChipMap[binding.chipSubPolity] = "Polity"
        subjectChipMap[binding.chipSubEconomics] = "Economics"

        subjectChipMap.forEach { (btn, subName) ->
            btn.setOnClickListener {
                selectSubject(subName)
            }
        }
    }

    private fun loadData() {
        allSubjects = practiceRepo.getCurriculum()
        if (allSubjects.isEmpty()) return

        // If configuring an existing widget, read its current subject
        var initialSubjectName = "History"
        var initialTopicName = ""
        var initialSetNum = 1

        if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            val config = CustomTopicWidgetProvider.getWidgetConfiguration(this, appWidgetId)
            initialSubjectName = config.first
            initialTopicName = config.second
            initialSetNum = config.third
        }

        selectSubject(initialSubjectName, initialTopicName, initialSetNum)
    }

    private fun selectSubject(
        subjectName: String,
        targetTopicName: String? = null,
        targetSetNum: Int? = null
    ) {
        val subject = allSubjects.firstOrNull {
            it.name.equals(subjectName, ignoreCase = true)
        } ?: allSubjects.firstOrNull() ?: return

        selectedSubject = subject

        // Update chip visuals
        subjectChipMap.forEach { (btn, name) ->
            if (name.equals(subject.name, ignoreCase = true)) {
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

        populateTopicsSpinner(subject, targetTopicName, targetSetNum)
    }

    private fun populateTopicsSpinner(
        subject: PracticeSubject,
        targetTopicName: String? = null,
        targetSetNum: Int? = null
    ) {
        val topics = subject.topics
        if (topics.isEmpty()) {
            binding.spinnerTopics.adapter = null
            binding.spinnerSets.adapter = null
            return
        }

        val topicNames = topics.map {
            "${it.displayName} (${it.totalQuestions} Qs)"
        }

        val adapter = ArrayAdapter(this, R.layout.item_spinner_selected, topicNames).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerTopics.adapter = adapter

        // Set initial selection
        var initialIdx = 0
        if (!targetTopicName.isNullOrEmpty()) {
            val idx = topics.indexOfFirst {
                it.displayName.equals(targetTopicName, ignoreCase = true) ||
                        it.categoryName.equals(targetTopicName, ignoreCase = true)
            }
            if (idx >= 0) initialIdx = idx
        }
        binding.spinnerTopics.setSelection(initialIdx)

        binding.spinnerTopics.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in topics.indices) {
                    val topic = topics[position]
                    selectedTopic = topic
                    populateSetsSpinner(topic, targetSetNum)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        if (initialIdx in topics.indices) {
            val topic = topics[initialIdx]
            selectedTopic = topic
            populateSetsSpinner(topic, targetSetNum)
        }
    }

    private fun populateSetsSpinner(topic: PracticeTopic, targetSetNum: Int? = null) {
        val sets = topic.sets
        if (sets.isEmpty()) {
            binding.spinnerSets.adapter = null
            return
        }

        val setLabels = sets.map { set ->
            "Set ${set.setNumber}: Q ${set.startRange}–${set.endRange} (${set.totalQuestions} Qs) • ${set.statusBadge}"
        }

        val adapter = ArrayAdapter(this, R.layout.item_spinner_selected, setLabels).apply {
            setDropDownViewResource(R.layout.item_spinner_dropdown)
        }
        binding.spinnerSets.adapter = adapter

        var initialIdx = 0
        if (targetSetNum != null) {
            val idx = sets.indexOfFirst { it.setNumber == targetSetNum }
            if (idx >= 0) initialIdx = idx
        }
        binding.spinnerSets.setSelection(initialIdx)

        binding.spinnerSets.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in sets.indices) {
                    selectedSet = sets[position]
                    updatePreview()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        if (initialIdx in sets.indices) {
            selectedSet = sets[initialIdx]
            updatePreview()
        }
    }

    private fun updatePreview() {
        val subject = selectedSubject
        val topic = selectedTopic
        val set = selectedSet

        if (subject != null && topic != null && set != null) {
            binding.tvPreviewSubject.text = "${subject.iconEmoji} ${subject.name}"
            binding.tvPreviewTopic.text = "Topic: ${topic.displayName}"
            binding.tvPreviewSet.text = "Set ${set.setNumber} • Q ${set.startRange} to ${set.endRange} (${set.totalQuestions} Questions)"
        }
    }

    private fun setupListeners(isPinMode: Boolean) {
        if (isPinMode) {
            binding.btnApplyWidget.text = "Add Widget to Home Screen 📌"
        } else {
            binding.btnApplyWidget.text = "Save & Update Widget ✓"
        }

        binding.btnApplyWidget.setOnClickListener {
            val subject = selectedSubject
            val topic = selectedTopic
            val set = selectedSet

            if (subject == null || topic == null || set == null) {
                Toast.makeText(this, "Please select a Subject, Topic and Set", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                // Updating or configuring existing widget
                CustomTopicWidgetProvider.configureWidget(
                    context = this,
                    appWidgetId = appWidgetId,
                    subject = subject.name,
                    topic = topic.displayName,
                    setNum = set.setNumber,
                    setKey = set.setKey,
                    questionIds = set.questionIds
                )

                val resultIntent = Intent().apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                }
                setResult(Activity.RESULT_OK, resultIntent)
                Toast.makeText(
                    this,
                    "Widget configured with ${subject.name}: Set ${set.setNumber}!",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                // In Pinning mode (from inside the app)
                CustomTopicWidgetProvider.saveDefaultSelection(
                    context = this,
                    subject = subject.name,
                    topic = topic.displayName,
                    setNum = set.setNumber,
                    setKey = set.setKey,
                    questionIds = set.questionIds
                )

                val appWidgetManager = AppWidgetManager.getInstance(this)
                if (appWidgetManager.isRequestPinAppWidgetSupported) {
                    val myProvider = ComponentName(this, CustomTopicWidgetProvider::class.java)
                    appWidgetManager.requestPinAppWidget(myProvider, null, null)
                    Toast.makeText(
                        this,
                        "Widget pinned request sent! Tap 'Add to home screen' on prompt.",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        "Selected ${subject.name} (Set ${set.setNumber})! Add Quizee widget from your home screen.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                finish()
            }
        }
    }
}
