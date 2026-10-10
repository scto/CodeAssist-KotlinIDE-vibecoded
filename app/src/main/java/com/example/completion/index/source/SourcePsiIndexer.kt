package com.example.completion.index.source

import com.example.completion.core.CompletionLogger
import com.example.completion.psi.KotlinParser
import com.example.completion.symbol.Symbol

import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Incremental Source Indexer that indexes Kotlin source files using Kotlin PSI/AST parser.
 */
class SourcePsiIndexer @JvmOverloads constructor(
    private val parser: KotlinParser,
    private val sourceSymbolIndex: SourceSymbolIndex,
    logger: CompletionLogger? = null
) {
    private val logger: CompletionLogger = logger ?: CompletionLogger.NoOpLogger()

    fun indexFile(file: File?) {
        if (file == null || !file.exists() || !file.isFile) return
        if (!file.name.endsWith(".kt") && !file.name.endsWith(".kts")) return

        try {
            val content = readFileContent(file)
            updateFile(file, content)
        } catch (e: Throwable) {
            logger.error("Failed to index source file: ${file.absolutePath}", e)
        }
    }

    fun updateFile(file: File?, newContent: String) {
        if (file == null) return
        val filePath = file.absolutePath
        val lastMod = file.lastModified()

        try {
            val parsed = parser.parse(file.name, newContent)
            val projectSymbols = ArrayList<ProjectSymbol>()

            for (s in parsed.topLevelSymbols) {
                projectSymbols.add(
                    ProjectSymbol.builder()
                        .name(s.name)
                        .qualifiedName(s.qualifiedName)
                        .kind(s.kind)
                        .origin(s.origin)
                        .packageName(s.packageName)
                        .returnType(s.returnType)
                        .parameters(s.parameters)
                        .containerName(s.containerName)
                        .receiverType(s.receiverType)
                        .sourceFilePath(filePath)
                        .lastModified(lastMod)
                        .build()
                )
            }

            sourceSymbolIndex.updateFileSymbols(filePath, projectSymbols)
            logger.debug("Indexed source file: ${file.name} (${projectSymbols.size} symbols)")
        } catch (e: Throwable) {
            logger.error("Failed to parse and index content for: $filePath", e)
        }
    }

    fun removeFile(file: File?) {
        if (file != null) {
            sourceSymbolIndex.removeFile(file.absolutePath)
        }
    }

    fun indexDirectoryRecursively(directory: File?) {
        if (directory == null || !directory.exists()) return
        if (directory.isFile) {
            indexFile(directory)
            return
        }

        val files = directory.listFiles()
        if (files != null) {
            for (f in files) {
                if (f.isDirectory) {
                    indexDirectoryRecursively(f)
                } else if (f.name.endsWith(".kt") || f.name.endsWith(".kts")) {
                    indexFile(f)
                }
            }
        }
    }

    private fun readFileContent(file: File): String {
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line).append("\n")
            }
        }
        return sb.toString()
    }
}
