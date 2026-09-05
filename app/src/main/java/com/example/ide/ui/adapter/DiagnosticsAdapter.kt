package com.example.ide.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.R
import com.example.databinding.ItemDiagnosticBinding
import com.example.ide.model.DiagnosticItem

class DiagnosticsAdapter(
    private var diagnostics: List<DiagnosticItem>,
    private val onItemClick: (DiagnosticItem) -> Unit,
    private val onQuickFixClick: (DiagnosticItem) -> Unit
) : RecyclerView.Adapter<DiagnosticsAdapter.DiagViewHolder>() {

    fun updateDiagnostics(newList: List<DiagnosticItem>) {
        diagnostics = newList
        notifyDataSetChanged()
    }

    class DiagViewHolder(val binding: ItemDiagnosticBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiagViewHolder {
        val binding = ItemDiagnosticBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DiagViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DiagViewHolder, position: Int) {
        val item = diagnostics[position]

        holder.binding.tvDiagMessage.text = item.message
        holder.binding.tvDiagLocation.text = "Line ${item.line}, Column ${item.column} (${item.rule})"

        when (item.severity) {
            DiagnosticItem.Severity.ERROR -> {
                holder.binding.ivDiagSeverity.setImageResource(R.drawable.ic_error)
                holder.binding.tvDiagMessage.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.status_error)
                )
            }
            DiagnosticItem.Severity.WARNING -> {
                holder.binding.ivDiagSeverity.setImageResource(R.drawable.ic_warning)
                holder.binding.tvDiagMessage.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.status_warning)
                )
            }
            DiagnosticItem.Severity.INFO,
            DiagnosticItem.Severity.HINT -> {
                holder.binding.ivDiagSeverity.setImageResource(R.drawable.ic_info)
                holder.binding.tvDiagMessage.setTextColor(
                    ContextCompat.getColor(holder.itemView.context, R.color.status_info)
                )
            }
        }

        if (item.quickFixText != null) {
            holder.binding.tvDiagQuickFix.visibility = View.VISIBLE
            holder.binding.tvDiagQuickFix.text = "💡 Quick Fix: ${item.quickFixText}"
            holder.binding.tvDiagQuickFix.setOnClickListener {
                onQuickFixClick(item)
            }
        } else {
            holder.binding.tvDiagQuickFix.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = diagnostics.size
}
