package com.example.completion.index.jar

import com.example.completion.symbol.Symbol

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Stores members (functions, fields, methods) extracted from JAR class files.
 */
class JarSymbolIndex {

    // Qualified Container Class Name -> List of Member Symbols
    private val classMembersMap: MutableMap<String, MutableList<Symbol>> = ConcurrentHashMap()

    fun addMember(containerClassName: String?, member: Symbol?) {
        if (containerClassName == null || member == null) return
        classMembersMap.computeIfAbsent(containerClassName) { CopyOnWriteArrayList() }.add(member)
    }

    fun addMembers(containerClassName: String?, members: List<Symbol>?) {
        if (containerClassName == null || members == null) return
        classMembersMap.computeIfAbsent(containerClassName) { CopyOnWriteArrayList() }.addAll(members)
    }

    fun getMembers(containerClassName: String?): List<Symbol> {
        if (containerClassName == null) return Collections.emptyList()
        val list = classMembersMap[containerClassName]
        return if (list != null) Collections.unmodifiableList(list) else Collections.emptyList()
    }

    fun removeMembersForClass(containerClassName: String?) {
        if (containerClassName != null) {
            classMembersMap.remove(containerClassName)
        }
    }

    fun clear() {
        classMembersMap.clear()
    }
}
