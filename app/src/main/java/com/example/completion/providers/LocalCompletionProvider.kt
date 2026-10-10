package com.example.completion.providers

import com.example.completion.core.CompletionItem
import com.example.completion.core.CompletionProvider
import com.example.completion.core.CompletionRequest
import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.psi.CompletionContextType
import com.example.completion.psi.KotlinPsiManager
import com.example.completion.psi.PsiContextResolver
import com.example.completion.resolver.LocalSymbolResolver

/**
 * Suggests in-scope local variables, parameters, and expressions visible at the cursor.
 */
class LocalCompletionProvider(
    private val localSymbolResolver: LocalSymbolResolver,
    private val psiManager: KotlinPsiManager,
    private val contextResolver: PsiContextResolver
) : CompletionProvider {

    override val id: String
        get() = "local-completion-provider"

    override val priority: Int
        get() = 900 // High priority

    override fun isApplicable(request: CompletionRequest): Boolean {
        val ctx = contextResolver.resolveContext(request, null)
        return ctx.contextType != CompletionContextType.MEMBER_ACCESS &&
                ctx.contextType != CompletionContextType.IMPORT &&
                ctx.contextType != CompletionContextType.PACKAGE &&
                ctx.contextType != CompletionContextType.STRING &&
                ctx.contextType != CompletionContextType.COMMENT
    }

    override fun complete(request: CompletionRequest): List<CompletionItem> {
        val parsed = psiManager.parse(request.filePath, request.content)
        val localSymbols = localSymbolResolver.resolveLocalSymbols(request.content, request.cursorPosition, parsed)

        val items = ArrayList<CompletionItem>()
        val prefix = request.prefix

        for (s in localSymbols) {
            if (prefix.isEmpty() || s.name.lowercase().startsWith(prefix.lowercase())) {
                items.add(
                    CompletionItem.builder(s.name, s.kind)
                        .insertText(s.name)
                        .detail(if (s.kind == SymbolKind.PARAMETER) "param: ${s.returnType}" else "val/var: ${s.returnType}")
                        .type(s.returnType)
                        .origin(SymbolOrigin.LOCAL)
                        .priority(500)
                        .build()
                )
            }
        }

        return items
    }
}
