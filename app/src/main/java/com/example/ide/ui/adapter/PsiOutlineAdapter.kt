package com.example.ide.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ItemPsiOutlineBinding
import com.example.ide.model.PsiOutlineNode

class PsiOutlineAdapter(
    private var nodes: List<PsiOutlineNode>,
    private val onNodeClick: (PsiOutlineNode) -> Unit
) : RecyclerView.Adapter<PsiOutlineAdapter.OutlineViewHolder>() {

    fun updateNodes(newNodes: List<PsiOutlineNode>) {
        nodes = newNodes
        notifyDataSetChanged()
    }

    class OutlineViewHolder(val binding: ItemPsiOutlineBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OutlineViewHolder {
        val binding = ItemPsiOutlineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OutlineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OutlineViewHolder, position: Int) {
        val node = nodes[position]
        holder.binding.tvKindBadge.text = when (node.kind) {
            PsiOutlineNode.OutlineKind.PACKAGE -> "PKG"
            PsiOutlineNode.OutlineKind.IMPORT,
            PsiOutlineNode.OutlineKind.IMPORT_GROUP -> "IMP"
            PsiOutlineNode.OutlineKind.CLASS -> "CLASS"
            PsiOutlineNode.OutlineKind.DATA_CLASS -> "DATA"
            PsiOutlineNode.OutlineKind.INTERFACE -> "INTF"
            PsiOutlineNode.OutlineKind.OBJECT,
            PsiOutlineNode.OutlineKind.COMPANION_OBJECT -> "OBJ"
            PsiOutlineNode.OutlineKind.FUNCTION -> "FUN"
            PsiOutlineNode.OutlineKind.PROPERTY,
            PsiOutlineNode.OutlineKind.VARIABLE -> "VAL"
            PsiOutlineNode.OutlineKind.ENUM_CLASS -> "ENUM"
            PsiOutlineNode.OutlineKind.CONSTRUCTOR -> "INIT"
        }

        holder.binding.tvPsiName.text = if (node.details.isNotEmpty()) "${node.name} : ${node.details}" else node.name
        holder.binding.tvPsiLine.text = "Ln ${node.line}"

        holder.itemView.setOnClickListener {
            onNodeClick(node)
        }
    }

    override fun getItemCount(): Int = nodes.size
}
