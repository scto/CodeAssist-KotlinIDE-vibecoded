package com.example.completion.index.jar;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stores indexed class declarations from external JAR files.
 */
public class JarClassIndex {

    // Lowercase simple name -> List of JarClassSymbol
    private final Map<String, List<JarClassSymbol>> simpleNameIndex = new ConcurrentHashMap<>();

    // Qualified name -> JarClassSymbol
    private final Map<String, JarClassSymbol> qualifiedNameIndex = new ConcurrentHashMap<>();

    // Jar path -> List of classes
    private final Map<String, List<JarClassSymbol>> jarFilesMap = new ConcurrentHashMap<>();

    public void addClass(JarClassSymbol symbol) {
        if (symbol == null) return;

        qualifiedNameIndex.put(symbol.getQualifiedName(), symbol);

        String lowerSimple = symbol.getName().toLowerCase(Locale.ROOT);
        simpleNameIndex.computeIfAbsent(lowerSimple, k -> new CopyOnWriteArrayList<>()).add(symbol);

        if (!symbol.getJarFilePath().isEmpty()) {
            jarFilesMap.computeIfAbsent(symbol.getJarFilePath(), k -> new CopyOnWriteArrayList<>()).add(symbol);
        }
    }

    public void removeJar(String jarPath) {
        if (jarPath == null) return;
        List<JarClassSymbol> removed = jarFilesMap.remove(jarPath);
        if (removed != null) {
            for (JarClassSymbol sym : removed) {
                qualifiedNameIndex.remove(sym.getQualifiedName());
                String lowerSimple = sym.getName().toLowerCase(Locale.ROOT);
                List<JarClassSymbol> list = simpleNameIndex.get(lowerSimple);
                if (list != null) {
                    list.remove(sym);
                    if (list.isEmpty()) {
                        simpleNameIndex.remove(lowerSimple);
                    }
                }
            }
        }
    }

    public JarClassSymbol findByQualifiedName(String qName) {
        return qName != null ? qualifiedNameIndex.get(qName) : null;
    }

    public List<JarClassSymbol> findByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>(qualifiedNameIndex.values());
        }
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        List<JarClassSymbol> results = new ArrayList<>();
        for (Map.Entry<String, List<JarClassSymbol>> entry : simpleNameIndex.entrySet()) {
            if (entry.getKey().startsWith(lowerPrefix)) {
                results.addAll(entry.getValue());
            }
        }
        return results;
    }

    public List<JarClassSymbol> getAllClasses() {
        return new ArrayList<>(qualifiedNameIndex.values());
    }

    public void clear() {
        simpleNameIndex.clear();
        qualifiedNameIndex.clear();
        jarFilesMap.clear();
    }
}
