package com.example.ide.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.databinding.ItemEditorTabBinding
import com.example.ide.model.EditorTab

class EditorTabAdapter(
    private val tabs: List<EditorTab>,
    private var activeIndex: Int,
    private val onTabClick: (Int) -> Unit,
    private val onTabClose: (Int) -> Unit
) : RecyclerView.Adapter<EditorTabAdapter.TabViewHolder>() {

    class TabViewHolder(val binding: ItemEditorTabBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TabViewHolder {
        val binding = ItemEditorTabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TabViewHolder, position: Int) {
        val tab = tabs[position]
        val isSelected = position == activeIndex

        holder.binding.tvTabTitle.text = tab.title
        holder.binding.tabContainer.setBackgroundResource(
            if (isSelected) R.drawable.bg_tab_selected else R.drawable.bg_tab_unselected
        )
        holder.binding.tvTabTitle.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (isSelected) R.color.ide_text_primary else R.color.ide_text_secondary
            )
        )
        holder.binding.viewModifiedDot.visibility = if (tab.isModified) View.VISIBLE else View.GONE

        // Icon based on extension
        when (tab.file.extension.lowercase()) {
            "kt", "kts" -> holder.binding.ivTabIcon.setImageResource(R.drawable.ic_kotlin_file)
            else -> holder.binding.ivTabIcon.setImageResource(R.drawable.ic_folder)
        }

        holder.binding.root.setOnClickListener {
            onTabClick(position)
        }

        holder.binding.btnTabClose.setOnClickListener {
            onTabClose(position)
        }
    }

    override fun getItemCount(): Int = tabs.size

    fun setActiveIndex(newIndex: Int) {
        val oldIndex = activeIndex
        activeIndex = newIndex
        if (oldIndex in tabs.indices) notifyItemChanged(oldIndex)
        if (newIndex in tabs.indices) notifyItemChanged(newIndex)
    }
}
