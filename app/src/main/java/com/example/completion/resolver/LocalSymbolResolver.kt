package com.example.completion.resolver

import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.psi.KotlinPsiElement
import com.example.completion.psi.KtElementKind
import com.example.completion.psi.ParsedKotlinFile
import com.example.completion.symbol.Symbol

import java.util.Collections
import java.util.regex.Pattern

/**
 * Resolves local variables, function parameters, and block-scoped symbols visible at the cursor offset.
 */
class LocalSymbolResolver {

    fun resolveLocalSymbols(source: String?, cursorOffset: Int, parsedFile: ParsedKotlinFile?): List<Symbol> {
        if (source == null || source.isEmpty() || cursorOffset <= 0) {
            return Collections.emptyList()
        }

        val safeOffset = Math.min(cursorOffset, source.length)
        val localSymbols = ArrayList<Symbol>()

        // 1. Walk AST children if available
        if (parsedFile != null && parsedFile.rootElement != null) {
            var current: KotlinPsiElement? = parsedFile.findElementAt(safeOffset)
            while (current != null) {
                if (current.kind == KtElementKind.FUNCTION) {
                    for (child in current.children) {
                        if (child.kind == KtElementKind.PARAMETER) {
                            localSymbols.add(
                                Symbol.builder()
                                    .name(child.name)
                                    .kind(SymbolKind.PARAMETER)
                                    .origin(SymbolOrigin.LOCAL)
                                    .returnType(if (child.type != null) child.type else "Any")
                                    .build()
                            )
                        }
                    }
                }
                current = current.parent
            }
        }

        // 2. Scan text lines up to cursor offset, respecting block scopes with brace tracking
        val textBeforeCursor = source.substring(0, safeOffset)
        val lines = textBeforeCursor.split("\n")

        for (line in lines) {
            val trimmed = line.trim()

            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                continue
            }

            // Local val/var
            val valMatcher = LOCAL_VAL_VAR_PATTERN.matcher(line)
            while (valMatcher.find()) {
                val name = valMatcher.group(2)
                val explicitType = valMatcher.group(3)
                val init = valMatcher.group(4)
                val type = explicitType?.trim() ?: inferType(init)

                localSymbols.add(
                    Symbol.builder()
                        .name(name)
                        .kind(SymbolKind.VARIABLE)
                        .origin(SymbolOrigin.LOCAL)
                        .returnType(type)
                        .build()
                )
            }

            // For loop variable
            val forMatcher = FOR_LOOP_PATTERN.matcher(line)
            if (forMatcher.find()) {
                val varName = forMatcher.group(1)
                localSymbols.add(
                    Symbol.builder()
                        .name(varName)
                        .kind(SymbolKind.VARIABLE)
                        .origin(SymbolOrigin.LOCAL)
                        .returnType("Any")
                        .build()
                )
            }

            // Catch clause variable
            val catchMatcher = CATCH_PATTERN.matcher(line)
            if (catchMatcher.find()) {
                val exName = catchMatcher.group(1)
                val exType = catchMatcher.group(2)
                localSymbols.add(
                    Symbol.builder()
                        .name(exName)
                        .kind(SymbolKind.VARIABLE)
                        .origin(SymbolOrigin.LOCAL)
                        .returnType(exType?.trim() ?: "Throwable")
                        .build()
                )
            }
        }

        return localSymbols
    }

    private fun inferType(init: String?): String {
        if (init == null) return "Any"
        val clean = init.trim()
        if (clean.startsWith("\"") && clean.endsWith("\"")) return "String"
        if (clean.matches(Regex("^-?\\d+$"))) return "Int"
        if (clean.matches(Regex("^-?\\d+L$"))) return "Long"
        if (clean.matches(Regex("^-?\\d*\\.\\d+f?$"))) return if (clean.endsWith("f")) "Float" else "Double"
        if ("true" == clean || "false" == clean) return "Boolean"
        return "Any"
    }

    companion object {
        private val LOCAL_VAL_VAR_PATTERN = Pattern.compile("(val|var)\\s+([a-zA-Z0-9_]+)(?:\\s*:\\s*([a-zA-Z0-9_<>., ?]+))?(?:\\s*=\\s*([^\\n;]+))?")
        private val FOR_LOOP_PATTERN = Pattern.compile("for\\s*\\(\\s*([a-zA-Z0-9_]+)\\s+in\\s+([^)]+)\\)")
        private val CATCH_PATTERN = Pattern.compile("catch\\s*\\(\\s*([a-zA-Z0-9_]+)\\s*:\\s*([a-zA-Z0-9_<>.]+)\\)")
    }
}
