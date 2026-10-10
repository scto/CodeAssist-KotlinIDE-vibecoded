package com.example.completion.index.jar

import com.example.completion.core.CompletionLogger

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages caching, background indexing, and timestamp verification for dependency JARs.
 */
class JarIndexManager @JvmOverloads constructor(
    private val jarIndexer: JarIndexer,
    private val classIndex: JarClassIndex,
    private val symbolIndex: JarSymbolIndex,
    logger: CompletionLogger? = null
) {
    private val logger: CompletionLogger = logger ?: CompletionLogger.NoOpLogger()
    private val indexedJarsTimestamp: MutableMap<String, Long> = ConcurrentHashMap()
    private val backgroundExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        val t = Thread(r, "JarIndexingWorker")
        t.isDaemon = true
        t.priority = Thread.MIN_PRIORITY
        t
    }

    fun indexJarIfNeeded(jarFile: File?) {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile) return

        val path = jarFile.absolutePath
        val lastModified = jarFile.lastModified()

        val previousMod = indexedJarsTimestamp[path]
        if (previousMod != null && previousMod == lastModified) {
            return // Up to date
        }

        jarIndexer.indexJar(jarFile)
        indexedJarsTimestamp[path] = lastModified
    }

    fun indexJarAsync(jarFile: File?) {
        backgroundExecutor.submit { indexJarIfNeeded(jarFile) }
    }

    fun isIndexed(jarFile: File?): Boolean {
        return jarFile != null && indexedJarsTimestamp.containsKey(jarFile.absolutePath)
    }

    fun invalidate(jarFile: File?) {
        if (jarFile != null) {
            val path = jarFile.absolutePath
            indexedJarsTimestamp.remove(path)
            classIndex.removeJar(path)
        }
    }

    fun clear() {
        indexedJarsTimestamp.clear()
        classIndex.clear()
        symbolIndex.clear()
    }
}
