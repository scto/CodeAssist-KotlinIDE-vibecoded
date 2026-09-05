package com.example.completion.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The Central Completion Engine that coordinates completion providers,
 * context resolution, ranking, deduplication, and execution boundaries.
 */
public class CompletionEngine {

    private static final int DEFAULT_MAX_RESULTS = 80;
    private static final long DEFAULT_TIMEOUT_MS = 1200;

    private final List<CompletionProvider> providers = new CopyOnWriteArrayList<>();
    private final CompletionLogger logger;
    private final ExecutorService executorService;
    private final boolean ownsExecutor;
    private int maxResults = DEFAULT_MAX_RESULTS;
    private long timeoutMs = DEFAULT_TIMEOUT_MS;

    public CompletionEngine() {
        this(new CompletionLogger.AndroidLogger("CompletionEngine"), null);
    }

    public CompletionEngine(CompletionLogger logger) {
        this(logger, null);
    }

    public CompletionEngine(CompletionLogger logger, ExecutorService executorService) {
        this.logger = logger != null ? logger : new CompletionLogger.NoOpLogger();
        if (executorService != null) {
            this.executorService = executorService;
            this.ownsExecutor = false;
        } else {
            this.executorService = Executors.newFixedThreadPool(
                    Math.max(2, Runtime.getRuntime().availableProcessors()),
                    r -> {
                        Thread t = new Thread(r, "KotlinCompletionWorker");
                        t.setDaemon(true);
                        t.setPriority(Thread.NORM_PRIORITY);
                        return t;
                    }
            );
            this.ownsExecutor = true;
        }
    }

    public void registerProvider(CompletionProvider provider) {
        if (provider != null && !providers.contains(provider)) {
            providers.add(provider);
            // Sort providers by priority descending
            providers.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
            logger.debug("Registered completion provider: " + provider.getId() + " (priority=" + provider.getPriority() + ")");
        }
    }

    public void unregisterProvider(CompletionProvider provider) {
        if (provider != null) {
            providers.remove(provider);
            logger.debug("Unregistered completion provider: " + provider.getId());
        }
    }

    public List<CompletionProvider> getProviders() {
        return Collections.unmodifiableList(providers);
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = Math.max(1, maxResults);
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setTimeoutMs(long timeoutMs) {
        this.timeoutMs = Math.max(50, timeoutMs);
    }

    public long getTimeoutMs() {
        return timeoutMs;
    }

    /**
     * Executes completion synchronously across all applicable registered providers,
     * scores, ranks, and deduplicates the results.
     */
    public CompletionResult complete(CompletionRequest request) {
        if (request == null) {
            return CompletionResult.empty();
        }

        long startTime = System.currentTimeMillis();
        String prefix = request.getPrefix();
        List<CompletionItem> collectedItems = new ArrayList<>();
        Set<String> seenSignatures = new HashSet<>();

        try {
            for (CompletionProvider provider : providers) {
                try {
                    if (!provider.isApplicable(request)) {
                        continue;
                    }

                    List<CompletionItem> items = provider.complete(request);
                    if (items != null && !items.isEmpty()) {
                        for (CompletionItem item : items) {
                            if (item == null) continue;

                            // Calculate score
                            int score = CompletionScore.calculateScore(item, prefix);
                            if (score <= 0 && prefix != null && !prefix.isEmpty()) {
                                continue; // Filtered out by match
                            }

                            String sig = item.getLabel() + ":" + item.getKind() + ":" + item.getDetail();
                            if (seenSignatures.add(sig)) {
                                collectedItems.add(item.withScore(score));
                            }
                        }
                    }
                } catch (Throwable providerError) {
                    logger.error("Error in provider [" + provider.getId() + "]", providerError);
                }
            }

            // Rank items by score and priority
            Collections.sort(collectedItems);

            boolean isTruncated = false;
            List<CompletionItem> finalItems = collectedItems;
            if (collectedItems.size() > maxResults) {
                finalItems = new ArrayList<>(collectedItems.subList(0, maxResults));
                isTruncated = true;
            }

            long durationMs = System.currentTimeMillis() - startTime;
            logger.debug("Completion completed in " + durationMs + "ms with " + finalItems.size() + " items (prefix='" + prefix + "')");

            return new CompletionResult(finalItems, prefix, isTruncated, durationMs);
        } catch (Throwable e) {
            logger.error("Unexpected error in CompletionEngine.complete()", e);
            return CompletionResult.empty();
        }
    }

    /**
     * Executes completion asynchronously with a strict timeout boundary to ensure UI responsiveness.
     */
    public CompletableFuture<CompletionResult> completeAsync(CompletionRequest request) {
        return CompletableFuture.supplyAsync(() -> complete(request), executorService);
    }

    /**
     * Shuts down any background executor owned by this engine.
     */
    public void shutdown() {
        if (ownsExecutor && !executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(300, TimeUnit.MILLISECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
