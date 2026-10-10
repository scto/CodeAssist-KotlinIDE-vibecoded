package com.example.completion.providers

import com.example.completion.core.CompletionItem
import com.example.completion.core.CompletionProvider
import com.example.completion.core.CompletionRequest
import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.index.repository.SymbolRepository
import com.example.completion.psi.CompletionContextType
import com.example.completion.psi.PsiContextResolver

/**
 * Provides package path completions for package directives.
 */
class PackageCompletionProvider(
    private val symbolRepository: SymbolRepository?,
    private val contextResolver: PsiContextResolver
) : CompletionProvider {

    override val id: String
        get() = "package-completion-provider"

    override val priority: Int
        get() = 950

    override fun isApplicable(request: CompletionRequest): Boolean {
        val ctx = contextResolver.resolveContext(request, null)
        return ctx.contextType == CompletionContextType.PACKAGE
    }

    override fun complete(request: CompletionRequest): List<CompletionItem> {
        val ctx = contextResolver.resolveContext(request, null)
        val prefix = ctx.prefix
        val items = ArrayList<CompletionItem>()

        if (symbolRepository == null) return items

        val lastDot = prefix.lastIndexOf('.')
        for (pkg in symbolRepository.getMatchingPackages(prefix)) {
            val label = if (lastDot != -1) pkg.substring(lastDot + 1) else pkg
            items.add(
                CompletionItem.builder(label, SymbolKind.PACKAGE)
                    .insertText(label)
                    .detail(pkg)
                    .origin(SymbolOrigin.SOURCE)
                    .priority(300)
                    .build()
            )
        }

        return items
    }
}
