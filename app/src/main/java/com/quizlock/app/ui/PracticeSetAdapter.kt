package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.R
import com.quizlock.app.data.PracticeSet
import com.quizlock.app.databinding.ItemPracticeSetBinding

class PracticeSetAdapter(
    private var sets: List<PracticeSet>,
    private val onSetClick: (PracticeSet) -> Unit
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
            binding.tvSetSubtitle.text = "${set.totalQuestions} Questions • Sequential Curriculum"

            if (set.isCompleted) {
                binding.btnStartSet.text = "Retest 🔁"
                binding.btnStartSet.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.card_dark_elevated)
                binding.btnStartSet.setTextColor(ContextCompat.getColor(ctx, R.color.primary_light))

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
                binding.btnStartSet.text = "Start ⚡"
                binding.btnStartSet.backgroundTintList = ContextCompat.getColorStateList(ctx, R.color.primary)
                binding.btnStartSet.setTextColor(ContextCompat.getColor(ctx, R.color.white))

                binding.tvSetStatusBadge.text = "Unattempted"
                binding.tvSetStatusBadge.setTextColor(ContextCompat.getColor(ctx, R.color.text_muted))
            }

            binding.btnStartSet.setOnClickListener {
                onSetClick(set)
            }

            binding.cardSetRoot.setOnClickListener {
                onSetClick(set)
            }
        }
    }
}
