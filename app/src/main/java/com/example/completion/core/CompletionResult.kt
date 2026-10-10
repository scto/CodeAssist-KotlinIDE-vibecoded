package com.example.completion.core

data class CompletionResult(
    val items: List<CompletionItem>,
    val prefix: String = "",
    val isTruncated: Boolean = false,
    val durationMs: Long = 0
) {
    val isEmpty: Boolean get() = items.isEmpty()
    val size: Int get() = items.size

    companion object {
        val EMPTY = CompletionResult(emptyList(), "", false, 0)

        fun empty(): CompletionResult = EMPTY

        fun of(items: List<CompletionItem>, prefix: String, durationMs: Long): CompletionResult {
            return CompletionResult(items, prefix, false, durationMs)
        }
    }
}
