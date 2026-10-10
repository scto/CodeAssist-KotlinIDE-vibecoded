package com.example.completion.providers

import com.example.completion.core.CompletionItem
import com.example.completion.core.CompletionProvider
import com.example.completion.core.CompletionRequest
import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.index.repository.SymbolRepository
import com.example.completion.psi.CompletionContextType
import com.example.completion.psi.KotlinPsiManager
import com.example.completion.psi.PsiContextResolver
import com.example.completion.symbol.Symbol

/**
 * Suggests functions, properties, and symbols defined across project source files and current file.
 */
class SourceCompletionProvider(
    private val symbolRepository: SymbolRepository?,
    private val psiManager: KotlinPsiManager,
    private val contextResolver: PsiContextResolver
) : CompletionProvider {

    override val id: String
        get() = "source-completion-provider"

    override val priority: Int
        get() = 700

    override fun isApplicable(request: CompletionRequest): Boolean {
        val ctx = contextResolver.resolveContext(request, null)
        return ctx.contextType != CompletionContextType.MEMBER_ACCESS &&
                ctx.contextType != CompletionContextType.IMPORT &&
                ctx.contextType != CompletionContextType.PACKAGE &&
                ctx.contextType != CompletionContextType.STRING &&
                ctx.contextType != CompletionContextType.COMMENT
    }

    override fun complete(request: CompletionRequest): List<CompletionItem> {
        val items = ArrayList<CompletionItem>()
        val seen = HashSet<String>()
        val prefix = request.prefix

        // 1. Current File top-level symbols
        val parsed = psiManager.parse(request.filePath, request.content)
        if (parsed != null) {
            for (s in parsed.topLevelSymbols) {
                if (prefix.isEmpty() || s.name.lowercase().startsWith(prefix.lowercase())) {
                    if (seen.add("${s.name}:${s.kind}")) {
                        val insertText = if (s.kind == SymbolKind.FUNCTION) "${s.name}()" else s.name
                        val detail = if (s.kind == SymbolKind.FUNCTION) "fun ${s.name}${s.parameterSignature}: ${s.returnType}" else s.returnType
                        items.add(
                            CompletionItem.builder(s.name, s.kind)
                                .insertText(insertText)
                                .detail(detail)
                                .type(s.returnType)
                                .origin(SymbolOrigin.SOURCE)
                                .priority(350)
                                .build()
                        )
                    }
                }
            }
        }

        // 2. Project workspace source symbols
        if (symbolRepository != null) {
            for (s in symbolRepository.search(prefix)) {
                if (s.origin == SymbolOrigin.SOURCE || s.origin == SymbolOrigin.BUILTIN) {
                    if (seen.add("${s.name}:${s.kind}")) {
                        val insertText = if (s.kind == SymbolKind.FUNCTION) "${s.name}()" else s.name
                        val detail = if (s.kind == SymbolKind.FUNCTION) "fun ${s.name}${s.parameterSignature}: ${s.returnType}" else s.returnType
                        items.add(
                            CompletionItem.builder(s.name, s.kind)
                                .insertText(insertText)
                                .detail(detail)
                                .type(s.returnType)
                                .origin(s.origin)
                                .priority(250)
                                .build()
                        )
                    }
                }
            }
        }

        return items
    }
}
