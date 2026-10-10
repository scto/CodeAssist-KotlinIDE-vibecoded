package com.example.completion.resolver

import com.example.completion.index.repository.SymbolRepository
import com.example.completion.psi.ParsedKotlinFile
import com.example.completion.symbol.Symbol

import java.util.Collections

/**
 * Resolves members, methods, fields, and extension functions for dot-qualified receiver expressions.
 */
class MemberResolver(
    private val typeResolver: SimpleTypeResolver,
    private val symbolRepository: SymbolRepository?
) {

    fun resolveMembers(receiverExpr: String?, source: String, cursorOffset: Int, parsedFile: ParsedKotlinFile?): List<Symbol> {
        if (receiverExpr == null || receiverExpr.trim().isEmpty() || symbolRepository == null) {
            return Collections.emptyList()
        }

        val typeResult = typeResolver.resolveReceiverType(receiverExpr, source, cursorOffset, parsedFile)
        val typeName = typeResult.typeName

        val members = ArrayList<Symbol>()
        val seen = HashSet<String>()

        // 1. Direct members for type
        for (s in symbolRepository.findMembers(typeName)) {
            if (seen.add("${s.name}:${s.kind}")) {
                members.add(s)
            }
        }

        // 2. Extension functions matching receiver type
        for (ext in symbolRepository.findExtensionFunctions(typeName)) {
            if (seen.add("${ext.name}:${ext.kind}")) {
                members.add(ext)
            }
        }

        // 3. Fallback Any members
        if (typeName != "Any") {
            for (anyMember in symbolRepository.findMembers("Any")) {
                if (seen.add("${anyMember.name}:${anyMember.kind}")) {
                    members.add(anyMember)
                }
            }
        }

        return members
    }
}
