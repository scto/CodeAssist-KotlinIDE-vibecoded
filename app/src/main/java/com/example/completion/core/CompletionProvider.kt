package com.example.completion.core

interface CompletionProvider {
    val id: String
    val priority: Int

    fun isApplicable(request: CompletionRequest): Boolean
    fun complete(request: CompletionRequest): List<CompletionItem>
}
