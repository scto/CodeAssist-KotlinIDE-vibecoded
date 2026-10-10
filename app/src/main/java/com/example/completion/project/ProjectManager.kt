package com.example.completion.project

import com.example.completion.core.CompletionLogger
import com.example.completion.index.source.SourcePsiIndexer

import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Manages local project workspace structure, source root tracking, and directory indexing.
 */
class ProjectManager @JvmOverloads constructor(
    private val sourcePsiIndexer: SourcePsiIndexer,
    logger: CompletionLogger? = null
) {
    private val logger: CompletionLogger = logger ?: CompletionLogger.NoOpLogger()
    var projectRoot: File? = null
        set(value) {
            field = value
            if (value != null && value.exists() && value.isDirectory) {
                executor.submit {
                    logger.info("Starting recursive indexing of project: ${value.absolutePath}")
                    sourcePsiIndexer.indexDirectoryRecursively(value)
                    logger.info("Finished indexing project: ${value.name}")
                }
            }
        }

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        val t = Thread(r, "ProjectIndexWorker")
        t.isDaemon = true
        t
    }

    fun onFileModified(file: File?, newContent: String) {
        if (file != null) {
            sourcePsiIndexer.updateFile(file, newContent)
        }
    }

    fun onFileDeleted(file: File?) {
        if (file != null) {
            sourcePsiIndexer.removeFile(file)
        }
    }
}
