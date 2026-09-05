package com.example.completion.sora;

import com.example.completion.core.CompletionRequest;
import com.example.completion.core.CompletionResult;
import com.example.completion.project.KotlinCompletionFacade;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Publisher service bridging Sora editor text changes to completion engine candidate stream.
 */
public class SoraKotlinCompletionPublisher {

    private final KotlinCompletionFacade completionFacade;
    private final SoraCompletionAdapter adapter;

    public SoraKotlinCompletionPublisher(KotlinCompletionFacade completionFacade) {
        this.completionFacade = completionFacade;
        this.adapter = new SoraCompletionAdapter();
    }

    public CompletableFuture<List<SoraCompletionItem>> query(String filePath, String content, int cursorOffset) {
        CompletionRequest request = CompletionRequest.create(filePath, content, cursorOffset);
        return completionFacade.completeAsync(request)
                .thenApply(adapter::adapt);
    }
}
