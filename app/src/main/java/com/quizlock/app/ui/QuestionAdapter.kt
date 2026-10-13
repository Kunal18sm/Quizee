package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.data.Question
import com.quizlock.app.databinding.ItemQuestionBinding

class QuestionAdapter(
    private val onDeleteClicked: (question: Question) -> Unit
) : RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder>() {

    private val questions = mutableListOf<Question>()

    fun submitList(list: List<Question>) {
        questions.clear()
        questions.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position])
    }

    override fun getItemCount(): Int = questions.size

    inner class QuestionViewHolder(private val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(question: Question) {
            binding.tvQuestionCategory.text = question.category
            binding.tvQuestionTitle.text = question.question

            val correctLetter = when (question.correctIndex) {
                0 -> "A"
                1 -> "B"
                2 -> "C"
                3 -> "D"
                else -> "?"
            }
            val correctText = if (question.correctIndex in question.options.indices) {
                question.options[question.correctIndex]
            } else ""

            binding.tvCorrectAnswerPreview.text = "✓ Correct Answer: Option $correctLetter ($correctText)"

            val optionsPreview = question.options.mapIndexed { idx, opt ->
                val letter = ('A' + idx)
                "$letter) $opt"
            }.joinToString("  |  ")

            binding.tvAllOptionsPreview.text = optionsPreview

            binding.ivDeleteQuestion.setOnClickListener {
                onDeleteClicked(question)
            }
        }
    }
}
