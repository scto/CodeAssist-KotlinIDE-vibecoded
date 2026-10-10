package com.example.completion.index.jar

import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Stores indexed class declarations from external JAR files.
 */
class JarClassIndex {

    // Lowercase simple name -> List of JarClassSymbol
    private val simpleNameIndex: MutableMap<String, MutableList<JarClassSymbol>> = ConcurrentHashMap()

    // Qualified name -> JarClassSymbol
    private val qualifiedNameIndex: MutableMap<String, JarClassSymbol> = ConcurrentHashMap()

    // Jar path -> List of classes
    private val jarFilesMap: MutableMap<String, MutableList<JarClassSymbol>> = ConcurrentHashMap()

    fun addClass(symbol: JarClassSymbol?) {
        if (symbol == null) return

        qualifiedNameIndex[symbol.qualifiedName] = symbol

        val lowerSimple = symbol.name.lowercase(Locale.ROOT)
        simpleNameIndex.computeIfAbsent(lowerSimple) { CopyOnWriteArrayList() }.add(symbol)

        if (symbol.jarFilePath.isNotEmpty()) {
            jarFilesMap.computeIfAbsent(symbol.jarFilePath) { CopyOnWriteArrayList() }.add(symbol)
        }
    }

    fun removeJar(jarPath: String?) {
        if (jarPath == null) return
        val removed = jarFilesMap.remove(jarPath)
        if (removed != null) {
            for (sym in removed) {
                qualifiedNameIndex.remove(sym.qualifiedName)
                val lowerSimple = sym.name.lowercase(Locale.ROOT)
                val list = simpleNameIndex[lowerSimple]
                if (list != null) {
                    list.remove(sym)
                    if (list.isEmpty()) {
                        simpleNameIndex.remove(lowerSimple)
                    }
                }
            }
        }
    }

    fun findByQualifiedName(qName: String?): JarClassSymbol? {
        return if (qName != null) qualifiedNameIndex[qName] else null
    }

    fun findByPrefix(prefix: String?): List<JarClassSymbol> {
        if (prefix == null || prefix.isEmpty()) {
            return ArrayList(qualifiedNameIndex.values)
        }
        val lowerPrefix = prefix.lowercase(Locale.ROOT)
        val results = ArrayList<JarClassSymbol>()
        for ((key, value) in simpleNameIndex) {
            if (key.startsWith(lowerPrefix)) {
                results.addAll(value)
            }
        }
        return results
    }

    fun getAllClasses(): List<JarClassSymbol> {
        return ArrayList(qualifiedNameIndex.values)
    }

    fun clear() {
        simpleNameIndex.clear()
        qualifiedNameIndex.clear()
        jarFilesMap.clear()
    }
}
