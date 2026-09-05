package com.example.completion.index.jar;

import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stores members (functions, fields, methods) extracted from JAR class files.
 */
public class JarSymbolIndex {

    // Qualified Container Class Name -> List of Member Symbols
    private final Map<String, List<Symbol>> classMembersMap = new ConcurrentHashMap<>();

    public void addMember(String containerClassName, Symbol member) {
        if (containerClassName == null || member == null) return;
        classMembersMap.computeIfAbsent(containerClassName, k -> new CopyOnWriteArrayList<>()).add(member);
    }

    public void addMembers(String containerClassName, List<Symbol> members) {
        if (containerClassName == null || members == null) return;
        classMembersMap.computeIfAbsent(containerClassName, k -> new CopyOnWriteArrayList<>()).addAll(members);
    }

    public List<Symbol> getMembers(String containerClassName) {
        if (containerClassName == null) return Collections.emptyList();
        List<Symbol> list = classMembersMap.get(containerClassName);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public void removeMembersForClass(String containerClassName) {
        if (containerClassName != null) {
            classMembersMap.remove(containerClassName);
        }
    }

    public void clear() {
        classMembersMap.clear();
    }
}
