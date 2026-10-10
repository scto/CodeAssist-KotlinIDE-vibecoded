package com.example.completion.index.source

import java.util.Collections
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Thread-safe in-memory symbol index storing symbols from all source files in the project workspace.
 */
class SourceSymbolIndex {

    // File path -> List of symbols in that file
    private val fileSymbolsMap: MutableMap<String, List<ProjectSymbol>> = ConcurrentHashMap()

    // Lowercase prefix index for fast search
    private val nameIndex: MutableMap<String, MutableList<ProjectSymbol>> = ConcurrentHashMap()

    fun updateFileSymbols(filePath: String?, symbols: List<ProjectSymbol>?) {
        if (filePath == null) return

        // Remove old symbols
        removeFile(filePath)

        if (symbols == null || symbols.isEmpty()) return

        val copy: List<ProjectSymbol> = CopyOnWriteArrayList(symbols)
        fileSymbolsMap[filePath] = copy

        for (s in copy) {
            val lowerName = s.name.lowercase(Locale.ROOT)
            nameIndex.computeIfAbsent(lowerName) { CopyOnWriteArrayList() }.add(s)
        }
    }

    fun removeFile(filePath: String?) {
        if (filePath == null) return
        val removed = fileSymbolsMap.remove(filePath)
        if (removed != null) {
            for (s in removed) {
                val lowerName = s.name.lowercase(Locale.ROOT)
                val list = nameIndex[lowerName]
                if (list != null) {
                    list.remove(s)
                    if (list.isEmpty()) {
                        nameIndex.remove(lowerName)
                    }
                }
            }
        }
    }

    fun findByPrefix(prefix: String?): List<ProjectSymbol> {
        if (prefix == null || prefix.isEmpty()) {
            return getAllSymbols()
        }
        val lowerPrefix = prefix.lowercase(Locale.ROOT)
        val results = ArrayList<ProjectSymbol>()
        for ((key, value) in nameIndex) {
            if (key.startsWith(lowerPrefix)) {
                results.addAll(value)
            }
        }
        return results
    }

    fun getSymbolsForFile(filePath: String?): List<ProjectSymbol> {
        if (filePath == null) return Collections.emptyList()
        val list = fileSymbolsMap[filePath]
        return if (list != null) Collections.unmodifiableList(list) else Collections.emptyList()
    }

    fun getAllSymbols(): List<ProjectSymbol> {
        val all = ArrayList<ProjectSymbol>()
        for (list in fileSymbolsMap.values) {
            all.addAll(list)
        }
        return all
    }

    fun clear() {
        fileSymbolsMap.clear()
        nameIndex.clear()
    }
}
