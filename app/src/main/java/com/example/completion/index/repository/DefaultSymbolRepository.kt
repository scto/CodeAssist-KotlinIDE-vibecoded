package com.example.completion.index.repository

import com.example.completion.index.jar.JarClassIndex
import com.example.completion.index.jar.JarClassSymbol
import com.example.completion.index.jar.JarSymbolIndex
import com.example.completion.index.metadata.KotlinMetadataIndexer
import com.example.completion.index.pkg.PackageIndex
import com.example.completion.index.source.SourceSymbolIndex
import com.example.completion.symbol.Symbol

import java.util.Collections
import java.util.Locale

/**
 * Unified implementation of SymbolRepository querying across all indexes.
 */
class DefaultSymbolRepository(
    private val sourceSymbolIndex: SourceSymbolIndex?,
    private val jarClassIndex: JarClassIndex?,
    private val jarSymbolIndex: JarSymbolIndex?,
    private val metadataIndexer: KotlinMetadataIndexer?,
    packageIndex: PackageIndex?
) : SymbolRepository {

    val packageIndex: PackageIndex = packageIndex ?: PackageIndex()

    override fun search(prefix: String?): List<Symbol> {
        val lowerPrefix = prefix?.lowercase(Locale.ROOT) ?: ""
        val results = ArrayList<Symbol>()
        val seen = HashSet<String>()

        // 1. Source Symbols
        if (sourceSymbolIndex != null) {
            for (s in sourceSymbolIndex.findByPrefix(prefix)) {
                if (seen.add("${s.qualifiedName}:${s.kind}")) {
                    results.add(s)
                }
            }
        }

        // 2. Built-in Symbols
        for (b in BuiltInKotlinSymbols.getBuiltIns()) {
            if (b.name.lowercase(Locale.ROOT).startsWith(lowerPrefix)) {
                if (seen.add("${b.qualifiedName}:${b.kind}")) {
                    results.add(b)
                }
            }
        }

        // 3. JAR Class Symbols
        if (jarClassIndex != null) {
            for (j in jarClassIndex.findByPrefix(prefix)) {
                if (seen.add("${j.qualifiedName}:${j.kind}")) {
                    results.add(j)
                }
            }
        }

        // 4. Metadata Symbols
        if (metadataIndexer != null) {
            for (m in metadataIndexer.getAllMetadataSymbols()) {
                if (m.name.lowercase(Locale.ROOT).startsWith(lowerPrefix)) {
                    if (seen.add("${m.qualifiedName}:${m.kind}")) {
                        results.add(m)
                    }
                }
            }
        }

        return results
    }

    override fun searchClasses(prefix: String?): List<JarClassSymbol> {
        return jarClassIndex?.findByPrefix(prefix) ?: Collections.emptyList()
    }

    override fun findMembers(containerClassName: String?): List<Symbol> {
        if (containerClassName == null || containerClassName.isEmpty()) {
            return Collections.emptyList()
        }

        val members = ArrayList<Symbol>()
        val seen = HashSet<String>()

        // Built-in members
        for (b in BuiltInKotlinSymbols.getMembersForType(containerClassName)) {
            if (seen.add(b.name)) {
                members.add(b)
            }
        }

        // JAR members
        if (jarSymbolIndex != null) {
            for (j in jarSymbolIndex.getMembers(containerClassName)) {
                if (seen.add(j.name)) {
                    members.add(j)
                }
            }
        }

        // Source members
        if (sourceSymbolIndex != null) {
            for (s in sourceSymbolIndex.getAllSymbols()) {
                if (containerClassName == s.containerName) {
                    if (seen.add(s.name)) {
                        members.add(s)
                    }
                }
            }
        }

        return members
    }

    override fun findExtensionFunctions(receiverType: String?): List<Symbol> {
        if (receiverType == null || receiverType.isEmpty()) return Collections.emptyList()
        val results = ArrayList<Symbol>()

        if (sourceSymbolIndex != null) {
            for (s in sourceSymbolIndex.getAllSymbols()) {
                if (s.isExtension && (receiverType == s.receiverType || "Any" == s.receiverType)) {
                    results.add(s)
                }
            }
        }

        return results
    }

    override fun getMatchingPackages(prefix: String?): List<String> {
        return packageIndex.getMatchingPackages(prefix)
    }

    override fun getClassesInPackage(packageName: String?, classPrefix: String?): List<String> {
        return packageIndex.getClassesInPackage(packageName, classPrefix)
    }
}
