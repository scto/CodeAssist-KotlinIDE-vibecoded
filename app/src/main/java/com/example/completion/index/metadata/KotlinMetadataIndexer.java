package com.example.completion.index.metadata;

import com.example.completion.core.CompletionLogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Indexes Kotlin declarations and metadata from compiled libraries.
 */
public class KotlinMetadataIndexer {

    private final KotlinMetadataReader metadataReader;
    private final CompletionLogger logger;
    private final Map<String, List<MetadataSymbol>> metadataSymbolsByClass = new ConcurrentHashMap<>();

    public KotlinMetadataIndexer(KotlinMetadataReader metadataReader, CompletionLogger logger) {
        this.metadataReader = metadataReader;
        this.logger = logger != null ? logger : new CompletionLogger.NoOpLogger();
    }

    public void indexClassFile(File classFile) {
        if (classFile == null || !classFile.exists() || !classFile.getName().endsWith(".class")) return;
        try (InputStream in = new FileInputStream(classFile)) {
            indexClassStream(classFile.getName(), in);
        } catch (Throwable e) {
            logger.error("Failed to index class file metadata: " + classFile.getAbsolutePath(), e);
        }
    }

    public void indexClassStream(String identifier, InputStream in) {
        try {
            List<MetadataSymbol> symbols = metadataReader.readSymbols(in);
            if (!symbols.isEmpty()) {
                metadataSymbolsByClass.put(identifier, new CopyOnWriteArrayList<>(symbols));
            }
        } catch (Throwable e) {
            logger.error("Failed to index class metadata stream for: " + identifier, e);
        }
    }

    public List<MetadataSymbol> getSymbolsForClass(String className) {
        if (className == null) return Collections.emptyList();
        List<MetadataSymbol> list = metadataSymbolsByClass.get(className);
        return list != null ? Collections.unmodifiableList(list) : Collections.emptyList();
    }

    public List<MetadataSymbol> getAllMetadataSymbols() {
        List<MetadataSymbol> all = new ArrayList<>();
        for (List<MetadataSymbol> list : metadataSymbolsByClass.values()) {
            all.addAll(list);
        }
        return all;
    }

    public void clear() {
        metadataSymbolsByClass.clear();
    }
}
