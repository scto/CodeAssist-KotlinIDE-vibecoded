package com.example.completion.index.jar

import com.example.completion.core.CompletionLogger
import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin

import java.io.File
import java.util.jar.JarFile

/**
 * Indexes bytecode entries from JAR dependency files directly without Reflection or ClassLoader.
 */
class JarIndexer @JvmOverloads constructor(
    private val classIndex: JarClassIndex,
    private val symbolIndex: JarSymbolIndex,
    logger: CompletionLogger? = null
) {
    private val logger: CompletionLogger = logger ?: CompletionLogger.NoOpLogger()

    fun indexJar(jarFile: File?) {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile) return

        val path = jarFile.absolutePath
        var indexedClasses = 0
        var indexedMembers = 0

        try {
            JarFile(jarFile).use { jar ->
                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val name = entry.name

                    if (name.endsWith(".class") && !name.contains("$") && !name.startsWith("META-INF/")) {
                        try {
                            jar.getInputStream(entry).use { inStream ->
                                val info = ClassFileReader.parse(inStream)
                                if (info != null && info.className.isNotEmpty()) {
                                    var packageName = ""
                                    val lastDot = info.className.lastIndexOf('.')
                                    if (lastDot != -1) {
                                        packageName = info.className.substring(0, lastDot)
                                    }

                                    val kind = if (info.isInterface) SymbolKind.INTERFACE else if (info.isEnum) SymbolKind.ENUM else SymbolKind.CLASS

                                    val classSymbol = JarClassSymbol.builder()
                                        .name(ClassFileReader.getSimpleName(info.className))
                                        .qualifiedName(info.className)
                                        .kind(kind)
                                        .origin(SymbolOrigin.JAR)
                                        .packageName(packageName)
                                        .jarFilePath(path)
                                        .isInterface(info.isInterface)
                                        .isEnum(info.isEnum)
                                        .build()

                                    classIndex.addClass(classSymbol)
                                    indexedClasses++

                                    if (info.declaredMembers.isNotEmpty()) {
                                        symbolIndex.addMembers(info.className, info.declaredMembers)
                                        indexedMembers += info.declaredMembers.size
                                    }
                                }
                            }
                        } catch (ignored: Throwable) {
                            // Skip malformed individual entries
                        }
                    }
                }
            }
            logger.debug("Indexed JAR: ${jarFile.name} -> $indexedClasses classes, $indexedMembers members")
        } catch (e: Throwable) {
            logger.error("Failed to index JAR file: $path", e)
        }
    }
}
