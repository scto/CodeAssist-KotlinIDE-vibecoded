package com.example.completion.index.jar;

import com.example.completion.core.CompletionLogger;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Manages caching, background indexing, and timestamp verification for dependency JARs.
 */
public class JarIndexManager {

    private final JarIndexer jarIndexer;
    private final JarClassIndex classIndex;
    private final JarSymbolIndex symbolIndex;
    private final CompletionLogger logger;
    private final Map<String, Long> indexedJarsTimestamp = new ConcurrentHashMap<>();
    private final ExecutorService backgroundExecutor;

    public JarIndexManager(JarIndexer jarIndexer, JarClassIndex classIndex, JarSymbolIndex symbolIndex, CompletionLogger logger) {
        this.jarIndexer = jarIndexer;
        this.classIndex = classIndex;
        this.symbolIndex = symbolIndex;
        this.logger = logger != null ? logger : new CompletionLogger.NoOpLogger();
        this.backgroundExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "JarIndexingWorker");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        });
    }

    public void indexJarIfNeeded(File jarFile) {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) return;

        String path = jarFile.getAbsolutePath();
        long lastModified = jarFile.lastModified();

        Long previousMod = indexedJarsTimestamp.get(path);
        if (previousMod != null && previousMod == lastModified) {
            return; // Up to date
        }

        jarIndexer.indexJar(jarFile);
        indexedJarsTimestamp.put(path, lastModified);
    }

    public void indexJarAsync(File jarFile) {
        backgroundExecutor.submit(() -> indexJarIfNeeded(jarFile));
    }

    public boolean isIndexed(File jarFile) {
        return jarFile != null && indexedJarsTimestamp.containsKey(jarFile.getAbsolutePath());
    }

    public void invalidate(File jarFile) {
        if (jarFile != null) {
            String path = jarFile.getAbsolutePath();
            indexedJarsTimestamp.remove(path);
            classIndex.removeJar(path);
        }
    }

    public void clear() {
        indexedJarsTimestamp.clear();
        classIndex.clear();
        symbolIndex.clear();
    }
}
