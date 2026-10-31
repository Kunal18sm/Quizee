package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.R
import com.quizlock.app.databinding.ItemMockPaletteChipBinding

class MockPaletteAdapter(
    private val totalCount: Int = 100,
    private val selectedOptions: IntArray,
    private val markedForReview: BooleanArray,
    private var currentIndex: Int,
    private val onQuestionClicked: (Int) -> Unit
) : RecyclerView.Adapter<MockPaletteAdapter.PaletteViewHolder>() {

    class PaletteViewHolder(val binding: ItemMockPaletteChipBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaletteViewHolder {
        val binding = ItemMockPaletteChipBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PaletteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaletteViewHolder, position: Int) {
        val qNum = position + 1
        holder.binding.tvChipNumber.text = qNum.toString()

        val context = holder.itemView.context
        val isCurrent = (position == currentIndex)
        val isMarked = markedForReview[position]
        val isAnswered = (selectedOptions[position] != -1)

        when {
            isCurrent -> {
                holder.binding.tvChipNumber.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_palette_current)
                holder.binding.tvChipNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            isMarked -> {
                holder.binding.tvChipNumber.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_palette_marked)
                holder.binding.tvChipNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            isAnswered -> {
                holder.binding.tvChipNumber.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_palette_answered)
                holder.binding.tvChipNumber.setTextColor(ContextCompat.getColor(context, R.color.white))
            }
            else -> {
                holder.binding.tvChipNumber.background =
                    ContextCompat.getDrawable(context, R.drawable.bg_palette_unattempted)
                holder.binding.tvChipNumber.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }
        }

        holder.itemView.setOnClickListener {
            onQuestionClicked(position)
        }
    }

    override fun getItemCount(): Int = totalCount

    fun updateCurrentIndex(newIndex: Int) {
        val oldIndex = currentIndex
        currentIndex = newIndex
        if (oldIndex in 0 until totalCount) notifyItemChanged(oldIndex)
        if (newIndex in 0 until totalCount) notifyItemChanged(newIndex)
    }
}
