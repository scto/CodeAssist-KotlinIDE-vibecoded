package com.example.completion.index.metadata

import com.example.completion.core.CompletionLogger

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Indexes Kotlin declarations and metadata from compiled libraries.
 */
class KotlinMetadataIndexer @JvmOverloads constructor(
    private val metadataReader: KotlinMetadataReader,
    logger: CompletionLogger? = null
) {
    private val logger: CompletionLogger = logger ?: CompletionLogger.NoOpLogger()
    private val metadataSymbolsByClass: MutableMap<String, List<MetadataSymbol>> = ConcurrentHashMap()

    fun indexClassFile(classFile: File?) {
        if (classFile == null || !classFile.exists() || !classFile.name.endsWith(".class")) return
        try {
            FileInputStream(classFile).use { inStream ->
                indexClassStream(classFile.name, inStream)
            }
        } catch (e: Throwable) {
            logger.error("Failed to index class file metadata: ${classFile.absolutePath}", e)
        }
    }

    fun indexClassStream(identifier: String, inStream: InputStream?) {
        try {
            val symbols = metadataReader.readSymbols(inStream)
            if (symbols.isNotEmpty()) {
                metadataSymbolsByClass[identifier] = CopyOnWriteArrayList(symbols)
            }
        } catch (e: Throwable) {
            logger.error("Failed to index class metadata stream for: $identifier", e)
        }
    }

    fun getSymbolsForClass(className: String?): List<MetadataSymbol> {
        if (className == null) return Collections.emptyList()
        val list = metadataSymbolsByClass[className]
        return if (list != null) Collections.unmodifiableList(list) else Collections.emptyList()
    }

    fun getAllMetadataSymbols(): List<MetadataSymbol> {
        val all = ArrayList<MetadataSymbol>()
        for (list in metadataSymbolsByClass.values) {
            all.addAll(list)
        }
        return all
    }

    fun clear() {
        metadataSymbolsByClass.clear()
    }
}
