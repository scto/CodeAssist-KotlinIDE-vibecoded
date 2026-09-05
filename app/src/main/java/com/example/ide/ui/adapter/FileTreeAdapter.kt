package com.example.ide.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.databinding.ItemFileTreeBinding
import com.example.ide.model.FileItem

class FileTreeAdapter(
    private var rootItem: FileItem,
    private val onFileClick: (FileItem) -> Unit,
    private val onNewFileUnder: (FileItem) -> Unit,
    private val onNewDirUnder: (FileItem) -> Unit,
    private val onRenameItem: (FileItem) -> Unit,
    private val onDeleteItem: (FileItem) -> Unit
) : RecyclerView.Adapter<FileTreeAdapter.FileTreeViewHolder>() {

    private val flatList = mutableListOf<FileItem>()

    init {
        rebuildFlatList()
    }

    fun updateRoot(newRoot: FileItem) {
        rootItem = newRoot
        rebuildFlatList()
        notifyDataSetChanged()
    }

    private fun rebuildFlatList() {
        flatList.clear()
        flatten(rootItem)
    }

    private fun flatten(item: FileItem) {
        flatList.add(item)
        if (item.isDirectory && item.isExpanded) {
            for (child in item.children) {
                flatten(child)
            }
        }
    }

    class FileTreeViewHolder(val binding: ItemFileTreeBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileTreeViewHolder {
        val binding = ItemFileTreeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FileTreeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FileTreeViewHolder, position: Int) {
        val item = flatList[position]

        // Indent level
        val indentPx = (item.level * 16 * holder.itemView.resources.displayMetrics.density).toInt()
        holder.binding.viewIndent.layoutParams = holder.binding.viewIndent.layoutParams.apply {
            width = indentPx
        }

        holder.binding.tvFileName.text = item.name

        if (item.isDirectory) {
            holder.binding.ivExpandArrow.visibility = View.VISIBLE
            holder.binding.ivExpandArrow.setImageResource(
                if (item.isExpanded) R.drawable.ic_expand_more else R.drawable.ic_chevron_right
            )
            holder.binding.ivFileIcon.setImageResource(
                if (item.isExpanded) R.drawable.ic_folder else R.drawable.ic_folder
            )
            holder.binding.tvFileName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.ide_text_primary)
            )
        } else {
            holder.binding.ivExpandArrow.visibility = View.INVISIBLE
            when {
                item.isKotlinFile -> holder.binding.ivFileIcon.setImageResource(R.drawable.ic_kotlin_file)
                item.isGradleFile -> holder.binding.ivFileIcon.setImageResource(R.drawable.ic_settings)
                item.isXmlFile -> holder.binding.ivFileIcon.setImageResource(R.drawable.ic_info)
                else -> holder.binding.ivFileIcon.setImageResource(R.drawable.ic_kotlin_file)
            }
            holder.binding.tvFileName.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.ide_text_secondary)
            )
        }

        holder.itemView.setOnClickListener {
            if (item.isDirectory) {
                item.isExpanded = !item.isExpanded
                rebuildFlatList()
                notifyDataSetChanged()
            } else {
                onFileClick(item)
            }
        }

        holder.binding.btnFileMenu.setOnClickListener { v ->
            val popup = PopupMenu(v.context, v)
            if (item.isDirectory) {
                popup.menu.add("New Kotlin File")
                popup.menu.add("New Folder")
            }
            popup.menu.add("Rename")
            popup.menu.add("Delete")

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "New Kotlin File" -> onNewFileUnder(item)
                    "New Folder" -> onNewDirUnder(item)
                    "Rename" -> onRenameItem(item)
                    "Delete" -> onDeleteItem(item)
                }
                true
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = flatList.size
}
