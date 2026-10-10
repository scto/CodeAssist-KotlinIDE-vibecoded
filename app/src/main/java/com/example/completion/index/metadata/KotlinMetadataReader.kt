package com.example.completion.index.metadata

import com.example.completion.core.CompletionLogger
import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.index.jar.ClassFileReader
import com.example.completion.symbol.Symbol

import java.io.InputStream
import java.util.Collections

/**
 * Reads and interprets Kotlin metadata annotations from compiled .class files.
 */
class KotlinMetadataReader @JvmOverloads constructor(logger: CompletionLogger? = null) {

    private val logger: CompletionLogger = logger ?: CompletionLogger.NoOpLogger()

    fun readSymbols(classInputStream: InputStream?): List<MetadataSymbol> {
        try {
            val info = ClassFileReader.parse(classInputStream)
            if (info == null || info.className.isEmpty()) {
                return Collections.emptyList()
            }

            val result = ArrayList<MetadataSymbol>()
            val simpleName = ClassFileReader.getSimpleName(info.className)
            var pkg = ""
            val lastDot = info.className.lastIndexOf('.')
            if (lastDot != -1) {
                pkg = info.className.substring(0, lastDot)
            }

            // Add class symbol
            val kind = if (info.isInterface) SymbolKind.INTERFACE else if (info.isEnum) SymbolKind.ENUM else SymbolKind.CLASS
            result.add(
                MetadataSymbol.builder()
                    .name(simpleName)
                    .qualifiedName(info.className)
                    .kind(kind)
                    .origin(SymbolOrigin.KOTLIN_METADATA)
                    .packageName(pkg)
                    .kotlinPackage(pkg)
                    .returnType(info.className)
                    .build()
            )

            // Convert members to metadata symbols
            for (m in info.declaredMembers) {
                result.add(
                    MetadataSymbol.builder()
                        .name(m.name)
                        .qualifiedName(m.qualifiedName)
                        .kind(m.kind)
                        .origin(SymbolOrigin.KOTLIN_METADATA)
                        .packageName(pkg)
                        .kotlinPackage(pkg)
                        .containerName(info.className)
                        .returnType(m.returnType)
                        .parameters(m.parameters)
                        .receiverType(m.receiverType)
                        .build()
                )
            }

            return result
        } catch (e: Throwable) {
            logger.error("Failed to read Kotlin metadata", e)
            return Collections.emptyList()
        }
    }
}
