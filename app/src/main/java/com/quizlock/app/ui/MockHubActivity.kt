package com.quizlock.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.quizlock.app.R
import com.quizlock.app.data.PracticeRepository
import com.quizlock.app.databinding.ActivityMockHubBinding

class MockHubActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMockHubBinding
    private lateinit var practiceRepo: PracticeRepository
    private lateinit var mockAdapter: MockTestAdapter

    private var activeLevel: Int = 2
    private val levelChipMap = mutableMapOf<MaterialButton, Int>()

    companion object {
        fun start(context: Context, initialLevel: Int = 2) {
            val intent = Intent(context, MockHubActivity::class.java).apply {
                putExtra("initial_level", initialLevel)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMockHubBinding.inflate(layoutInflater)
        setContentView(binding.root)

        practiceRepo = PracticeRepository.getInstance(this)
        activeLevel = intent.getIntExtra("initial_level", 2).coerceIn(1, 4)

        setupToolbar()
        setupMockRecyclerView()
        setupLevelChips()

        selectLevel(activeLevel)
    }

    override fun onResume() {
        super.onResume()
        selectLevel(activeLevel)
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupMockRecyclerView() {
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
    }

    private fun setupLevelChips() {
        levelChipMap[binding.chipLevel1] = 1
        levelChipMap[binding.chipLevel2] = 2
        levelChipMap[binding.chipLevel3] = 3
        levelChipMap[binding.chipLevel4] = 4

        levelChipMap.forEach { (btn, lvl) ->
            btn.setOnClickListener {
                selectLevel(lvl)
            }
        }
    }

    private fun selectLevel(level: Int) {
        activeLevel = level

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

        val levelName = when (level) {
            1 -> "Level 1: Foundation (Easy)"
            2 -> "Level 2: Standard CGL (Moderate)"
            3 -> "Level 3: Advanced (Hard)"
            4 -> "Level 4: Master Pro (Pro Ranker)"
            else -> "Level 2: Standard CGL"
        }

        binding.tvLevelSummary.text = "$levelName • Mocks 01 to 15"
        val mockList = practiceRepo.getAllMockTestsForLevel(level)
        mockAdapter.updateList(mockList)
    }
}
