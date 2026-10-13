package com.quizlock.app.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.quizlock.app.R
import com.quizlock.app.data.Question
import com.quizlock.app.data.QuestionRepository
import com.quizlock.app.databinding.ActivityQuestionManageBinding
import com.quizlock.app.databinding.DialogAddQuestionBinding

class QuestionManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQuestionManageBinding
    private lateinit var questionRepo: QuestionRepository
    private lateinit var adapter: QuestionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQuestionManageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        questionRepo = QuestionRepository.getInstance(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        setupRecyclerView()
        setupAddButton()
        loadQuestions()
    }

    private fun setupRecyclerView() {
        adapter = QuestionAdapter { question ->
            confirmDeleteQuestion(question)
        }
        binding.rvQuestions.layoutManager = LinearLayoutManager(this)
        binding.rvQuestions.adapter = adapter
    }

    private fun setupAddButton() {
        binding.fabAddQuestion.setOnClickListener {
            showAddQuestionDialog()
        }
    }

    private fun loadQuestions() {
        val list = questionRepo.getAllQuestions()
        binding.tvQuestionCount.text = "Question Bank (${list.size} Total)"
        adapter.submitList(list)
    }

    private fun confirmDeleteQuestion(question: Question) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Question?")
            .setMessage("Are you sure you want to delete:\n\n\"${question.question}\"?")
            .setPositiveButton("Delete") { _, _ ->
                questionRepo.deleteQuestion(question.id)
                loadQuestions()
                Toast.makeText(this, "Question removed", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddQuestionDialog() {
        val dialogBinding = DialogAddQuestionBinding.inflate(LayoutInflater.from(this))

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val addBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            addBtn.setOnClickListener {
                val qText = dialogBinding.etQuestionInput.text?.toString()?.trim() ?: ""
                val optA = dialogBinding.etOptionA.text?.toString()?.trim() ?: ""
                val optB = dialogBinding.etOptionB.text?.toString()?.trim() ?: ""
                val optC = dialogBinding.etOptionC.text?.toString()?.trim() ?: ""
                val optD = dialogBinding.etOptionD.text?.toString()?.trim() ?: ""

                if (qText.isEmpty() || optA.isEmpty() || optB.isEmpty() || optC.isEmpty() || optD.isEmpty()) {
                    Toast.makeText(this, "Please fill in the question and all 4 options!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val correctIdx = when (dialogBinding.rgCorrectOption.checkedRadioButtonId) {
                    R.id.rbOptionA -> 0
                    R.id.rbOptionB -> 1
                    R.id.rbOptionC -> 2
                    R.id.rbOptionD -> 3
                    else -> 0
                }

                val newQuestion = Question(
                    id = System.currentTimeMillis(),
                    question = qText,
                    options = listOf(optA, optB, optC, optD),
                    correctIndex = correctIdx,
                    category = "Custom"
                )

                questionRepo.addQuestion(newQuestion)
                loadQuestions()
                Toast.makeText(this, "Question added successfully!", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        dialog.show()
    }
}
