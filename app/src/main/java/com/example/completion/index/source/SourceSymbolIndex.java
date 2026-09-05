package com.example.completion.index.source;

import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe in-memory symbol index storing symbols from all source files in the project workspace.
 */
public class SourceSymbolIndex {

    // File path -> List of symbols in that file
    private final Map<String, List<ProjectSymbol>> fileSymbolsMap = new ConcurrentHashMap<>();

    // Lowercase prefix index for fast search
    private final Map<String, List<ProjectSymbol>> nameIndex = new ConcurrentHashMap<>();

    public void updateFileSymbols(String filePath, List<ProjectSymbol> symbols) {
        if (filePath == null) return;

        // Remove old symbols
        removeFile(filePath);

        if (symbols == null || symbols.isEmpty()) return;

        List<ProjectSymbol> copy = new CopyOnWriteArrayList<>(symbols);
        fileSymbolsMap.put(filePath, copy);

        for (ProjectSymbol s : copy) {
            String lowerName = s.getName().toLowerCase(Locale.ROOT);
            nameIndex.computeIfAbsent(lowerName, k -> new CopyOnWriteArrayList<>()).add(s);
        }
    }

    public void removeFile(String filePath) {
        if (filePath == null) return;
        List<ProjectSymbol> removed = fileSymbolsMap.remove(filePath);
        if (removed != null) {
            for (ProjectSymbol s : removed) {
                String lowerName = s.getName().toLowerCase(Locale.ROOT);
                List<ProjectSymbol> list = nameIndex.get(lowerName);
                if (list != null) {
                    list.remove(s);
                    if (list.isEmpty()) {
                        nameIndex.remove(lowerName);
                    }
                }
            }
        }
    }

    public List<ProjectSymbol> findByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return getAllSymbols();
        }
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<ProjectSymbol> results = new ArrayList<>();
        for (Map.Entry<String, List<ProjectSymbol>> entry : nameIndex.entrySet()) {
            if (entry.getKey().startsWith(lowerPrefix)) {
                results.addAll(entry.getValue());
            }
        }
        return results;
    }

    public List<ProjectSymbol> getSymbolsForFile(String filePath) {
        if (filePath == null) return Collections.emptyList();
        List<ProjectSymbol> list = fileSymbolsMap.get(filePath);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<ProjectSymbol> getAllSymbols() {
        List<ProjectSymbol> all = new ArrayList<>();
        for (List<ProjectSymbol> list : fileSymbolsMap.values()) {
            all.addAll(list);
        }
        return all;
    }

    public void clear() {
        fileSymbolsMap.clear();
        nameIndex.clear();
    }
}
