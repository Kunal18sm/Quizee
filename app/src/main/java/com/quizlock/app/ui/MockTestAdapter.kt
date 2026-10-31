package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.R
import com.quizlock.app.data.LevelMockTestMeta
import com.quizlock.app.databinding.ItemMockTestCardBinding

class MockTestAdapter(
    private var mockList: List<LevelMockTestMeta>,
    private val onStartExam: (LevelMockTestMeta) -> Unit,
    private val onStartPractice: (LevelMockTestMeta) -> Unit
) : RecyclerView.Adapter<MockTestAdapter.MockViewHolder>() {

    class MockViewHolder(val binding: ItemMockTestCardBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MockViewHolder {
        val binding = ItemMockTestCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MockViewHolder, position: Int) {
        val item = mockList[position]
        val context = holder.itemView.context

        holder.binding.tvCardMockTitle.text = "${item.levelName} • ${item.title}"
        holder.binding.tvCardMockBadge.text = item.levelBadge

        when (item.levelBadge.lowercase()) {
            "easy" -> holder.binding.tvCardMockBadge.setTextColor(ContextCompat.getColor(context, R.color.correct_green))
            "moderate" -> holder.binding.tvCardMockBadge.setTextColor(ContextCompat.getColor(context, R.color.warning_amber))
            "hard" -> holder.binding.tvCardMockBadge.setTextColor(ContextCompat.getColor(context, R.color.wrong_red))
            "pro ranker" -> holder.binding.tvCardMockBadge.setTextColor(ContextCompat.getColor(context, R.color.primary_light))
        }

        if (item.isCompleted) {
            holder.binding.tvCardMockStatus.text = "Score: ${item.formattedScore} / 200 (${item.accuracyPercent}%)"
            holder.binding.tvCardMockStatus.setTextColor(ContextCompat.getColor(context, R.color.correct_green))
        } else {
            holder.binding.tvCardMockStatus.text = "Unattempted"
            holder.binding.tvCardMockStatus.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
        }

        holder.binding.btnStartExamMode.setOnClickListener {
            onStartExam(item)
        }

        holder.binding.btnStartPracticeMode.setOnClickListener {
            onStartPractice(item)
        }
    }

    override fun getItemCount(): Int = mockList.size

    fun updateList(newList: List<LevelMockTestMeta>) {
        mockList = newList
        notifyDataSetChanged()
    }
}
