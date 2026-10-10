package com.example.completion.sora

import com.example.completion.core.CompletionItem
import com.example.completion.core.SymbolKind

/**
 * Adapter item holding metadata for display and insertion inside Sora Editor.
 */
class SoraCompletionItem(val coreItem: CompletionItem?) {

    val label: String
        get() = coreItem?.label ?: ""

    val insertText: String
        get() = coreItem?.insertText ?: ""

    val detail: String
        get() = coreItem?.detail ?: ""

    val kind: SymbolKind
        get() = coreItem?.kind ?: SymbolKind.VARIABLE

    val importToInsert: String?
        get() = coreItem?.importToInsert

    override fun toString(): String {
        return "$label ($detail)"
    }
}
