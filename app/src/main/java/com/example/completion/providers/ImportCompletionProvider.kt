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
 * Provides completion for import statements (subpackages and class names).
 */
class ImportCompletionProvider(
    private val symbolRepository: SymbolRepository?,
    private val contextResolver: PsiContextResolver
) : CompletionProvider {

    override val id: String
        get() = "import-completion-provider"

    override val priority: Int
        get() = 950

    override fun isApplicable(request: CompletionRequest): Boolean {
        val ctx = contextResolver.resolveContext(request, null)
        return ctx.contextType == CompletionContextType.IMPORT
    }

    override fun complete(request: CompletionRequest): List<CompletionItem> {
        val ctx = contextResolver.resolveContext(request, null)
        val prefix = ctx.prefix
        val items = ArrayList<CompletionItem>()

        if (symbolRepository == null) return items

        val lastDot = prefix.lastIndexOf('.')
        val parentPkg = if (lastDot != -1) prefix.substring(0, lastDot) else ""
        val subPrefix = if (lastDot != -1) prefix.substring(lastDot + 1) else prefix

        // 1. Suggest matching subpackages
        for (pkg in symbolRepository.getMatchingPackages(prefix)) {
            val label = if (lastDot != -1) pkg.substring(lastDot + 1) else pkg
            items.add(
                CompletionItem.builder(label, SymbolKind.PACKAGE)
                    .insertText(label)
                    .detail(pkg)
                    .origin(SymbolOrigin.JAR)
                    .priority(300)
                    .build()
            )
        }

        // 2. Suggest classes inside the package
        if (parentPkg.isNotEmpty()) {
            for (cls in symbolRepository.getClassesInPackage(parentPkg, subPrefix)) {
                items.add(
                    CompletionItem.builder(cls, SymbolKind.CLASS)
                        .insertText(cls)
                        .detail("$parentPkg.$cls")
                        .origin(SymbolOrigin.JAR)
                        .priority(400)
                        .build()
                )
            }
        }

        return items
    }
}
