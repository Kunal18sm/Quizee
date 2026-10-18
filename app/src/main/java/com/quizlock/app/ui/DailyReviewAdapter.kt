package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.R
import com.quizlock.app.data.Question
import com.quizlock.app.databinding.ItemDailyReviewQuestionBinding

class DailyReviewAdapter(
    private val masteredIds: Set<Long>,
    private val mistakeIds: Set<Long>,
    private val seenIds: Set<Long>
) : RecyclerView.Adapter<DailyReviewAdapter.ViewHolder>() {

    private val questions = mutableListOf<Question>()

    fun submitList(list: List<Question>) {
        questions.clear()
        questions.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemDailyReviewQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(questions[position], position + 1)
    }

    override fun getItemCount(): Int = questions.size

    inner class ViewHolder(private val binding: ItemDailyReviewQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(question: Question, number: Int) {
            val context = binding.root.context
            binding.tvQuestionBadge.text = "#$number • ${question.category}"
            binding.tvQuestionTitle.text = question.question

            // Status Badge
            when {
                question.id in masteredIds -> {
                    binding.tvStatusBadge.text = "✓ Mastered"
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.correct_green))
                    binding.tvStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.correct_green_light))
                }
                question.id in mistakeIds -> {
                    binding.tvStatusBadge.text = "⚠ Needs Revision"
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.wrong_red))
                    binding.tvStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.wrong_red_light))
                }
                question.id in seenIds -> {
                    binding.tvStatusBadge.text = "In Progress"
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_amber))
                    binding.tvStatusBadge.setBackgroundColor(ContextCompat.getColor(context, R.color.warning_amber_bg))
                }
                else -> {
                    binding.tvStatusBadge.text = "Unseen"
                    binding.tvStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.text_muted))
                    binding.tvStatusBadge.background = ContextCompat.getDrawable(context, R.drawable.bg_option_default)
                }
            }

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
        }
    }
}
