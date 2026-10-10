package com.example.completion.providers

import com.example.completion.core.CompletionItem
import com.example.completion.core.CompletionProvider
import com.example.completion.core.CompletionRequest
import com.example.completion.core.SymbolKind
import com.example.completion.psi.CompletionContextType
import com.example.completion.psi.KotlinPsiManager
import com.example.completion.psi.PsiContextResolver
import com.example.completion.resolver.MemberResolver

/**
 * Handles dot-qualified member completion (properties, methods, extension functions).
 */
class MemberCompletionProvider(
    private val memberResolver: MemberResolver,
    private val psiManager: KotlinPsiManager,
    private val contextResolver: PsiContextResolver
) : CompletionProvider {

    override val id: String
        get() = "member-completion-provider"

    override val priority: Int
        get() = 1000 // Top priority for member access

    override fun isApplicable(request: CompletionRequest): Boolean {
        val ctx = contextResolver.resolveContext(request, null)
        return ctx.contextType == CompletionContextType.MEMBER_ACCESS
    }

    override fun complete(request: CompletionRequest): List<CompletionItem> {
        val parsed = psiManager.parse(request.filePath, request.content)
        val ctx = contextResolver.resolveContext(request, parsed)

        val receiver = ctx.receiver
        val memberPrefix = ctx.prefix

        val members = memberResolver.resolveMembers(receiver, request.content, request.cursorPosition, parsed)
        val items = ArrayList<CompletionItem>()
        val seen = HashSet<String>()

        for (m in members) {
            if (memberPrefix.isEmpty() || m.name.lowercase().startsWith(memberPrefix.lowercase())) {
                if (seen.add("${m.name}:${m.kind}")) {
                    val insertText = if (m.kind == SymbolKind.FUNCTION) "${m.name}()" else m.name
                    val detail = if (m.kind == SymbolKind.FUNCTION) "fun ${m.name}${m.parameterSignature}: ${m.returnType}" else m.returnType

                    items.add(
                        CompletionItem.builder(m.name, m.kind)
                            .insertText(insertText)
                            .detail(detail)
                            .type(m.returnType)
                            .origin(m.origin)
                            .priority(600)
                            .build()
                    )
                }
            }
        }

        return items
    }
}
