package com.example.completion.core

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.max

class CompletionEngine(
    logger: CompletionLogger? = null,
    executorService: ExecutorService? = null
) {
    private val logger: CompletionLogger = logger ?: CompletionLogger.NoOpLogger()
    private val providers = CopyOnWriteArrayList<CompletionProvider>()
    
    private val executor: ExecutorService
    private val ownsExecutor: Boolean

    var maxResults: Int = 80
        set(value) { field = max(1, value) }

    var timeoutMs: Long = 1200
        set(value) { field = max(50L, value) }

    init {
        if (executorService != null) {
            this.executor = executorService
            this.ownsExecutor = false
        } else {
            this.executor = Executors.newFixedThreadPool(
                max(2, Runtime.getRuntime().availableProcessors())
            ) { r ->
                Thread(r, "KotlinCompletionWorker").apply {
                    isDaemon = true
                    priority = Thread.NORM_PRIORITY
                }
            }
            this.ownsExecutor = true
        }
    }

    fun registerProvider(provider: CompletionProvider?) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider)
            providers.sortByDescending { it.priority }
            logger.debug("Registered completion provider: ${provider.id} (priority=${provider.priority})")
        }
    }

    fun unregisterProvider(provider: CompletionProvider?) {
        if (provider != null && providers.remove(provider)) {
            logger.debug("Unregistered completion provider: ${provider.id}")
        }
    }

    fun getProviders(): List<CompletionProvider> = providers.toList()

    fun complete(request: CompletionRequest?): CompletionResult {
        if (request == null) return CompletionResult.empty()

        val startTime = System.currentTimeMillis()
        val prefix = request.prefix
        val collectedItems = mutableListOf<CompletionItem>()
        val seenSignatures = mutableSetOf<String>()

        try {
            for (provider in providers) {
                try {
                    if (!provider.isApplicable(request)) continue

                    val items = provider.complete(request)
                    for (item in items) {
                        val score = CompletionScore.calculateScore(item, prefix)
                        if (score <= 0 && prefix.isNotEmpty()) continue

                        val sig = "${item.label}:${item.kind}:${item.detail}"
                        if (seenSignatures.add(sig)) {
                            collectedItems.add(item.withScore(score))
                        }
                    }
                } catch (providerError: Throwable) {
                    logger.error("Error in provider [${provider.id}]", providerError)
                }
            }

            collectedItems.sort()

            val isTruncated = collectedItems.size > maxResults
            val finalItems = if (isTruncated) {
                collectedItems.subList(0, maxResults)
            } else {
                collectedItems
            }

            val durationMs = System.currentTimeMillis() - startTime
            logger.debug("Completion completed in ${durationMs}ms with ${finalItems.size} items (prefix='$prefix')")

            return CompletionResult(finalItems, prefix, isTruncated, durationMs)
        } catch (e: Throwable) {
            logger.error("Unexpected error in CompletionEngine.complete()", e)
            return CompletionResult.empty()
        }
    }

    /**
     * Führt die Completion asynchron als Coroutine aus.
     * Ersetzt das vorherige `CompletableFuture`-Konstrukt.
     */
    suspend fun completeAsync(request: CompletionRequest?): CompletionResult {
        return withContext(executor.asCoroutineDispatcher()) {
            withTimeoutOrNull(timeoutMs) {
                complete(request)
            } ?: CompletionResult.empty().also {
                logger.warn("Completion timed out after $timeoutMs ms")
            }
        }
    }

    fun shutdown() {
        if (ownsExecutor && !executor.isShutdown) {
            executor.shutdown()
            try {
                if (!executor.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executor.shutdownNow()
                Thread.currentThread().interrupt()
            }
        }
    }
}
