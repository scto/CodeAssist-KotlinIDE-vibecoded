package com.example.completion.psi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages parsed Kotlin files and coordinates caching and invalidation of PSI models.
 */
public class KotlinPsiManager {

    private final KotlinParser parser;
    private final Map<String, ParsedKotlinFile> fileCache = new ConcurrentHashMap<>();

    public KotlinPsiManager() {
        this(new DefaultKotlinPsiParser());
    }

    public KotlinPsiManager(KotlinParser parser) {
        this.parser = parser != null ? parser : new DefaultKotlinPsiParser();
    }

    public ParsedKotlinFile parse(String fileName, String source) {
        ParsedKotlinFile parsed = parser.parse(fileName, source);
        if (fileName != null) {
            fileCache.put(fileName, parsed);
        }
        return parsed;
    }

    public ParsedKotlinFile getCached(String fileName) {
        return fileName != null ? fileCache.get(fileName) : null;
    }

    public void invalidate(String fileName) {
        if (fileName != null) {
            fileCache.remove(fileName);
        }
    }

    public void clear() {
        fileCache.clear();
    }
}
