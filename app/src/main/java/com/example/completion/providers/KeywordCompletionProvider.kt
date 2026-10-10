package com.example.completion.providers

import com.example.completion.core.CompletionItem
import com.example.completion.core.CompletionProvider
import com.example.completion.core.CompletionRequest
import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.psi.CompletionContextType
import com.example.completion.psi.PsiContextResolver

/**
 * Provides Kotlin keywords completion when appropriate for the current context.
 */
class KeywordCompletionProvider @JvmOverloads constructor(
    private val contextResolver: PsiContextResolver = PsiContextResolver()
) : CompletionProvider {

    override val id: String
        get() = "keyword-completion-provider"

    override val priority: Int
        get() = 100

    override fun isApplicable(request: CompletionRequest): Boolean {
        val ctx = contextResolver.resolveContext(request, null)
        // Do not suggest keywords during member access ("obj.cl|"), string literals, or imports
        return ctx.contextType != CompletionContextType.MEMBER_ACCESS &&
                ctx.contextType != CompletionContextType.STRING &&
                ctx.contextType != CompletionContextType.COMMENT &&
                ctx.contextType != CompletionContextType.IMPORT &&
                ctx.contextType != CompletionContextType.PACKAGE
    }

    override fun complete(request: CompletionRequest): List<CompletionItem> {
        val prefix = request.prefix
        val items = ArrayList<CompletionItem>()

        for (kw in KEYWORDS) {
            if (prefix.isEmpty() || kw.startsWith(prefix.lowercase())) {
                items.add(
                    CompletionItem.builder(kw, SymbolKind.KEYWORD)
                        .insertText("$kw ")
                        .detail("keyword")
                        .origin(SymbolOrigin.KEYWORD)
                        .priority(10)
                        .build()
                )
            }
        }

        return items
    }

    companion object {
        private val KEYWORDS = listOf(
            "class", "interface", "object", "fun", "val", "var",
            "if", "else", "when", "for", "while", "do", "return",
            "package", "import", "public", "private", "protected",
            "internal", "override", "open", "abstract", "sealed",
            "data", "enum", "companion", "suspend", "inline",
            "try", "catch", "finally", "throw", "null", "true", "false",
            "is", "as", "in", "typealias", "constructor", "init"
        )
    }
}
