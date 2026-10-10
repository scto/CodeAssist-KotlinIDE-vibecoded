package com.example.completion.project

import com.example.completion.core.CompletionLogger
import com.example.completion.index.jar.JarIndexManager
import com.example.completion.index.pkg.PackageIndex

import java.io.File
import java.util.Collections
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Manages external JAR / AAR dependencies and triggers background indexing.
 */
class DependencyManager @JvmOverloads constructor(
    private val jarIndexManager: JarIndexManager,
    private val packageIndex: PackageIndex,
    logger: CompletionLogger? = null
) {
    private val logger: CompletionLogger = logger ?: CompletionLogger.NoOpLogger()
    private val dependencyFiles: MutableSet<File> = CopyOnWriteArraySet()

    fun addDependency(jarOrAarFile: File?) {
        if (jarOrAarFile == null || !jarOrAarFile.exists()) return
        dependencyFiles.add(jarOrAarFile)
        jarIndexManager.indexJarAsync(jarOrAarFile)
        logger.info("Added dependency: ${jarOrAarFile.name}")
    }

    fun addDependencies(files: List<File>?) {
        if (files == null) return
        for (f in files) {
            addDependency(f)
        }
    }

    fun removeDependency(jarFile: File?) {
        if (jarFile != null) {
            dependencyFiles.remove(jarFile)
            jarIndexManager.invalidate(jarFile)
        }
    }

    val dependencies: List<File>
        get() = Collections.unmodifiableList(ArrayList(dependencyFiles))

    fun clear() {
        dependencyFiles.clear()
        jarIndexManager.clear()
    }
}
