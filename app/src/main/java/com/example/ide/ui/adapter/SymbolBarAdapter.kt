package com.example.ide.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.databinding.ItemSymbolBtnBinding

class SymbolBarAdapter(
    private val symbols: List<String>,
    private val onSymbolClick: (String) -> Unit
) : RecyclerView.Adapter<SymbolBarAdapter.SymbolViewHolder>() {

    class SymbolViewHolder(val binding: ItemSymbolBtnBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolViewHolder {
        val binding = ItemSymbolBtnBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SymbolViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SymbolViewHolder, position: Int) {
        val sym = symbols[position]
        holder.binding.tvSymbolText.text = sym
        holder.binding.tvSymbolText.setOnClickListener {
            onSymbolClick(sym)
        }
    }

    override fun getItemCount(): Int = symbols.size
}
