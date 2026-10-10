package com.example.completion.providers

import com.example.completion.core.CompletionItem
import com.example.completion.core.CompletionProvider
import com.example.completion.core.CompletionRequest
import com.example.completion.core.SymbolOrigin
import com.example.completion.index.repository.SymbolRepository
import com.example.completion.psi.CompletionContextType
import com.example.completion.psi.PsiContextResolver

/**
 * Suggests class, interface, and object types with full package details and auto-import hints.
 */
class ClassCompletionProvider(
    private val symbolRepository: SymbolRepository?,
    private val contextResolver: PsiContextResolver
) : CompletionProvider {

    override val id: String
        get() = "class-completion-provider"

    override val priority: Int
        get() = 600

    override fun isApplicable(request: CompletionRequest): Boolean {
        val ctx = contextResolver.resolveContext(request, null)
        return ctx.contextType != CompletionContextType.MEMBER_ACCESS &&
                ctx.contextType != CompletionContextType.STRING &&
                ctx.contextType != CompletionContextType.COMMENT
    }

    override fun complete(request: CompletionRequest): List<CompletionItem> {
        val items = ArrayList<CompletionItem>()
        val seen = HashSet<String>()
        val prefix = request.prefix

        if (symbolRepository != null) {
            for (jc in symbolRepository.searchClasses(prefix)) {
                if (seen.add(jc.qualifiedName)) {
                    items.add(
                        CompletionItem.builder(jc.name, jc.kind)
                            .insertText(jc.name)
                            .detail(jc.qualifiedName)
                            .origin(SymbolOrigin.JAR)
                            .importToInsert(jc.qualifiedName)
                            .priority(200)
                            .build()
                    )
                }
            }
        }

        return items
    }
}
