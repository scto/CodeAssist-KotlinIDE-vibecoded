package com.example.completion.index.repository

import com.example.completion.index.jar.JarClassSymbol
import com.example.completion.symbol.Symbol

/**
 * Unified Repository interface querying symbols across Source, JAR, Metadata, and Built-ins.
 */
interface SymbolRepository {

    fun search(prefix: String?): List<Symbol>

    fun searchClasses(prefix: String?): List<JarClassSymbol>

    fun findMembers(containerClassName: String?): List<Symbol>

    fun findExtensionFunctions(receiverType: String?): List<Symbol>

    fun getMatchingPackages(prefix: String?): List<String>

    fun getClassesInPackage(packageName: String?, classPrefix: String?): List<String>
}
