package com.example.completion.sora

import com.example.completion.core.CompletionRequest
import com.example.completion.project.KotlinCompletionFacade

import java.util.concurrent.CompletableFuture

/**
 * Publisher service bridging Sora editor text changes to completion engine candidate stream.
 */
class SoraKotlinCompletionPublisher(private val completionFacade: KotlinCompletionFacade) {

    private val adapter: SoraCompletionAdapter = SoraCompletionAdapter()

    fun query(filePath: String, content: String, cursorOffset: Int): CompletableFuture<List<SoraCompletionItem>> {
        val request = CompletionRequest.create(content, cursorOffset, filePath)
        return completionFacade.completeAsync(request)
            .thenApply { result -> adapter.adapt(result) }
    }
}
