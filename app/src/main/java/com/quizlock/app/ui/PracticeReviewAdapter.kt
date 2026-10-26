package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.R
import com.quizlock.app.data.Question
import com.quizlock.app.databinding.ItemPracticeReviewBinding

data class PracticeReviewItem(
    val questionNumber: Int,
    val question: Question,
    val selectedIndex: Int, // -1 if skipped
    val isCorrect: Boolean
)

class PracticeReviewAdapter(
    private var items: List<PracticeReviewItem>
) : RecyclerView.Adapter<PracticeReviewAdapter.ViewHolder>() {

    fun updateList(newItems: List<PracticeReviewItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ItemPracticeReviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPracticeReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context
        val q = item.question

        holder.binding.tvReviewCategory.text = "Q${item.questionNumber} • ${q.category}"
        holder.binding.tvReviewQuestionText.text = q.question

        val correctOptionText = if (q.correctIndex in q.options.indices) {
            "${('A' + q.correctIndex)}. ${q.options[q.correctIndex]}"
        } else "N/A"

        holder.binding.tvReviewCorrectAnswer.text = correctOptionText

        when {
            item.selectedIndex == -1 -> {
                // Skipped
                holder.binding.tvReviewStatusBadge.text = "⏭️ Skipped"
                holder.binding.tvReviewStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.text_secondary)
                )
                holder.binding.layoutUserAnswer.visibility = View.GONE
                holder.binding.layoutCorrectAnswer.visibility = View.VISIBLE
            }
            item.isCorrect -> {
                // Correct
                holder.binding.tvReviewStatusBadge.text = "✓ Correct"
                holder.binding.tvReviewStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.correct_green)
                )
                holder.binding.layoutUserAnswer.visibility = View.GONE
                holder.binding.layoutCorrectAnswer.visibility = View.VISIBLE
            }
            else -> {
                // Wrong
                holder.binding.tvReviewStatusBadge.text = "✗ Incorrect"
                holder.binding.tvReviewStatusBadge.setTextColor(
                    ContextCompat.getColor(context, R.color.wrong_red)
                )
                holder.binding.layoutUserAnswer.visibility = View.VISIBLE
                val userOptionText = if (item.selectedIndex in q.options.indices) {
                    "${('A' + item.selectedIndex)}. ${q.options[item.selectedIndex]}"
                } else "N/A"
                holder.binding.tvReviewUserAnswer.text = userOptionText
                holder.binding.layoutCorrectAnswer.visibility = View.VISIBLE
            }
        }
    }

    override fun getItemCount(): Int = items.size
}
