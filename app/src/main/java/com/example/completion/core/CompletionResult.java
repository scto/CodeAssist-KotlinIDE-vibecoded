package com.example.completion.core;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Result model returned by the completion engine containing matching candidates.
 */
public final class CompletionResult {

    private static final CompletionResult EMPTY = new CompletionResult(Collections.emptyList(), "", false, 0);

    private final List<CompletionItem> items;
    private final String prefix;
    private final boolean isTruncated;
    private final long durationMs;

    public CompletionResult(List<CompletionItem> items, String prefix, boolean isTruncated, long durationMs) {
        this.items = items != null ? Collections.unmodifiableList(items) : Collections.emptyList();
        this.prefix = prefix != null ? prefix : "";
        this.isTruncated = isTruncated;
        this.durationMs = durationMs;
    }

    public List<CompletionItem> getItems() {
        return items;
    }

    public String getPrefix() {
        return prefix;
    }

    public boolean isTruncated() {
        return isTruncated;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public int size() {
        return items.size();
    }

    public static CompletionResult empty() {
        return EMPTY;
    }

    public static CompletionResult of(List<CompletionItem> items, String prefix, long durationMs) {
        return new CompletionResult(items, prefix, false, durationMs);
    }

    @Override
    public String toString() {
        return "CompletionResult{" +
                "itemsCount=" + items.size() +
                ", prefix='" + prefix + '\'' +
                ", isTruncated=" + isTruncated +
                ", durationMs=" + durationMs +
                '}';
    }
}
