package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.R
import com.quizlock.app.data.PracticeSet
import com.quizlock.app.data.PracticeTopic
import com.quizlock.app.databinding.ItemPracticeTopicBinding

class PracticeTopicAdapter(
    private var topics: List<PracticeTopic>,
    private val onSetClick: (set: PracticeSet, is50Count: Boolean) -> Unit,
    private val onTestAllTopicClick: (PracticeTopic) -> Unit,
    private val onSelectionChanged: (Set<String>) -> Unit
) : RecyclerView.Adapter<PracticeTopicAdapter.TopicViewHolder>() {

    val selectedCategories = mutableSetOf<String>()
    private val expandedCategories = mutableSetOf<String>()

    fun updateTopics(newTopics: List<PracticeTopic>) {
        topics = newTopics
        notifyDataSetChanged()
    }

    fun clearSelections() {
        selectedCategories.clear()
        notifyDataSetChanged()
        onSelectionChanged(selectedCategories)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopicViewHolder {
        val binding = ItemPracticeTopicBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TopicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TopicViewHolder, position: Int) {
        holder.bind(topics[position])
    }

    override fun getItemCount(): Int = topics.size

    inner class TopicViewHolder(private val binding: ItemPracticeTopicBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(topic: PracticeTopic) {
            val ctx = binding.root.context
            val isExpanded = expandedCategories.contains(topic.categoryName)
            val isChecked = selectedCategories.contains(topic.categoryName)

            binding.tvTopicTitle.text = topic.displayName
            binding.tvTopicMeta.text = "${topic.subjectName} • ${topic.totalQuestions} Questions • ${topic.sets.size} Sets"

            val completed = topic.completedSetsCount
            val totalSets = topic.sets.size
            binding.tvTopicProgressBadge.text = "$completed/$totalSets Sets Done"
            if (completed > 0) {
                binding.tvTopicProgressBadge.setTextColor(ContextCompat.getColor(ctx, R.color.correct_green))
            } else {
                binding.tvTopicProgressBadge.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }

            binding.pbTopicProgress.progress = topic.progressPercent

            // Checkbox
            binding.cbSelectTopic.setOnCheckedChangeListener(null)
            binding.cbSelectTopic.isChecked = isChecked
            binding.cbSelectTopic.setOnCheckedChangeListener { _, checked ->
                if (checked) {
                    selectedCategories.add(topic.categoryName)
                } else {
                    selectedCategories.remove(topic.categoryName)
                }
                onSelectionChanged(selectedCategories)
            }

            // Test All button
            val quickCount = topic.totalQuestions.coerceAtMost(50)
            binding.btnTestAllTopic.text = "Test All ($quickCount Qs)"
            binding.btnTestAllTopic.setOnClickListener {
                onTestAllTopicClick(topic)
            }

            // Expand/Collapse Sets
            binding.btnToggleSets.text = if (isExpanded) "Hide Sets ▲" else "View Sets (${topic.sets.size}) ▼"
            binding.layoutSetsContainer.visibility = if (isExpanded) View.VISIBLE else View.GONE

            if (isExpanded) {
                val setAdapter = PracticeSetAdapter(topic.sets) { set, is50Count ->
                    onSetClick(set, is50Count)
                }
                binding.rvTopicSets.layoutManager = LinearLayoutManager(ctx)
                binding.rvTopicSets.adapter = setAdapter
            }

            binding.btnToggleSets.setOnClickListener {
                if (isExpanded) {
                    expandedCategories.remove(topic.categoryName)
                } else {
                    expandedCategories.add(topic.categoryName)
                }
                notifyItemChanged(adapterPosition)
            }
        }
    }
}
