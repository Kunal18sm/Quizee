package com.quizlock.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quizlock.app.data.AppInfo
import com.quizlock.app.databinding.ItemAppBinding

class AppListAdapter(
    private val onLockChanged: (app: AppInfo, isLocked: Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    private val allApps = mutableListOf<AppInfo>()
    private val displayedApps = mutableListOf<AppInfo>()

    fun submitList(apps: List<AppInfo>) {
        allApps.clear()
        allApps.addAll(apps)
        displayedApps.clear()
        displayedApps.addAll(apps)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        displayedApps.clear()
        if (query.trim().isEmpty()) {
            displayedApps.addAll(allApps)
        } else {
            val lower = query.lowercase().trim()
            displayedApps.addAll(allApps.filter {
                it.name.lowercase().contains(lower) || it.packageName.lowercase().contains(lower)
            })
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(displayedApps[position])
    }

    override fun getItemCount(): Int = displayedApps.size

    inner class AppViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.ivAppIcon.setImageDrawable(app.icon)
            binding.tvAppName.text = app.name
            binding.tvAppPackage.text = app.packageName

            // Avoid triggering listener during bind
            binding.switchLock.setOnCheckedChangeListener(null)
            binding.switchLock.isChecked = app.isLocked

            binding.switchLock.setOnCheckedChangeListener { _, isChecked ->
                app.isLocked = isChecked
                onLockChanged(app, isChecked)
            }
        }
    }
}
