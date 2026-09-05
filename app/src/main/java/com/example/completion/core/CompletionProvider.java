package com.example.completion.core;

import java.util.List;

/**
 * Provider interface responsible for supplying completion items for a specific domain/context.
 */
public interface CompletionProvider {

    /**
     * Unique identifier for this provider (e.g., "keyword", "source-symbol", "member", "jar-class").
     */
    String getId();

    /**
     * Priority weight of this provider (higher priority runs earlier or gets evaluated first).
     */
    int getPriority();

    /**
     * Returns true if this provider is interested in providing completions for the given request.
     */
    boolean isApplicable(CompletionRequest request);

    /**
     * Produces completion items for the given request.
     * Implementations MUST be thread-safe and handle exceptions gracefully.
     *
     * @param request the completion request
     * @return list of completion items (may be empty, never null)
     */
    List<CompletionItem> complete(CompletionRequest request);
}
