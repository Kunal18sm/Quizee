package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.R
import com.quizlock.app.data.PracticeSet
import com.quizlock.app.databinding.ItemPracticeSetBinding

class PracticeSetAdapter(
    private var sets: List<PracticeSet>,
    private val onSetClick: (set: PracticeSet, is50Count: Boolean) -> Unit
) : RecyclerView.Adapter<PracticeSetAdapter.SetViewHolder>() {

    fun updateSets(newSets: List<PracticeSet>) {
        sets = newSets
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SetViewHolder {
        val binding = ItemPracticeSetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SetViewHolder, position: Int) {
        holder.bind(sets[position])
    }

    override fun getItemCount(): Int = sets.size

    inner class SetViewHolder(private val binding: ItemPracticeSetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(set: PracticeSet) {
            val ctx = binding.root.context

            binding.tvSetTitle.text = "Set ${set.setNumber} (Q ${set.startRange} – ${set.endRange})"
            binding.tvSetSubtitle.text = "${set.totalQuestions} Questions • Sequential Bank"

            // Status Badge
            if (set.isCompleted) {
                binding.btnStartFull.text = "Retest All (${set.totalQuestions}) 🔁"
                binding.btnStart50.text = "⚡ Retest 50 Qs"

                when {
                    set.accuracyPercent >= 80 -> {
                        binding.tvSetStatusBadge.text = "🏆 Mastered (${set.accuracyPercent}%)"
                        binding.tvSetStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.correct_green))
                    }
                    set.accuracyPercent >= 50 -> {
                        binding.tvSetStatusBadge.text = "🎯 Good (${set.accuracyPercent}%)"
                        binding.tvSetStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.primary_light))
                    }
                    else -> {
                        binding.tvSetStatusBadge.text = "⚠️ Needs Work (${set.accuracyPercent}%)"
                        binding.tvSetStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.warning_amber))
                    }
                }
            } else {
                binding.btnStartFull.text = "📖 Full Set (${set.totalQuestions} Qs)"
                binding.btnStart50.text = "⚡ 50 Qs Drill"

                binding.tvSetStatusBadge.text = "Unattempted"
                binding.tvSetStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }

            // If set has 50 or fewer questions, 50 Qs drill button is redundant
            if (set.totalQuestions <= 50) {
                binding.btnStart50.visibility = View.GONE
                binding.btnStartFull.text = "Start (${set.totalQuestions} Qs) ⚡"
            } else {
                binding.btnStart50.visibility = View.VISIBLE
            }

            // Click Handlers
            binding.btnStart50.setOnClickListener {
                onSetClick(set, true)
            }

            binding.btnStartFull.setOnClickListener {
                onSetClick(set, false)
            }
        }
    }
}
