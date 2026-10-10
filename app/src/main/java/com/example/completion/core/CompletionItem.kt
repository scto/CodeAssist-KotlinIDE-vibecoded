package com.example.completion.core

data class CompletionItem(
    val label: String,
    val kind: SymbolKind = SymbolKind.UNKNOWN,
    val insertText: String = label,
    val detail: String = "",
    val documentation: String = "",
    val origin: SymbolOrigin = SymbolOrigin.SOURCE,
    val score: Int = 0,
    val priority: Int = 0,
    val importToInsert: String? = null,
    val type: String = ""
) : Comparable<CompletionItem> {

    fun withScore(newScore: Int): CompletionItem = copy(score = newScore)

    companion object {
        @JvmStatic
        fun builder(label: String, kind: SymbolKind = SymbolKind.UNKNOWN): Builder = Builder(label, kind)
    }

    /** Fluent Builder für die Provider (Java-Stil-API). */
    class Builder(private val label: String, private val kind: SymbolKind) {
        private var insertText: String? = null
        private var detail: String? = null
        private var documentation: String? = null
        private var origin: SymbolOrigin? = null
        private var score: Int = 0
        private var priority: Int = 0
        private var importToInsert: String? = null
        private var type: String? = null

        fun insertText(value: String?): Builder = apply { insertText = value }
        fun detail(value: String?): Builder = apply { detail = value }
        fun documentation(value: String?): Builder = apply { documentation = value }
        fun origin(value: SymbolOrigin?): Builder = apply { origin = value }
        fun score(value: Int): Builder = apply { score = value }
        fun priority(value: Int): Builder = apply { priority = value }
        fun importToInsert(value: String?): Builder = apply { importToInsert = value }
        fun type(value: String?): Builder = apply { type = value }

        fun build(): CompletionItem = CompletionItem(
            label = label,
            kind = kind,
            insertText = insertText ?: label,
            detail = detail ?: "",
            documentation = documentation ?: "",
            origin = origin ?: SymbolOrigin.SOURCE,
            score = score,
            priority = priority,
            importToInsert = importToInsert,
            type = type ?: ""
        )
    }

    override fun compareTo(other: CompletionItem): Int {
        // Höchster Score zuerst
        if (this.score != other.score) {
            return other.score.compareTo(this.score)
        }
        // Höchste Priorität zuerst
        if (this.priority != other.priority) {
            return other.priority.compareTo(this.priority)
        }
        // Alphabetisch nach Label
        return this.label.compareTo(other.label, ignoreCase = true)
    }
}
