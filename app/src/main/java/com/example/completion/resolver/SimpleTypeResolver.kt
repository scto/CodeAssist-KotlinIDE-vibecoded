package com.example.completion.resolver

import com.example.completion.psi.ParsedKotlinFile
import com.example.completion.symbol.Symbol

/**
 * Heuristic, lightweight type resolution engine for receiver expressions.
 */
class SimpleTypeResolver @JvmOverloads constructor(
    private val localSymbolResolver: LocalSymbolResolver = LocalSymbolResolver()
) {

    fun resolveReceiverType(receiverExpr: String?, source: String, cursorOffset: Int, parsedFile: ParsedKotlinFile?): TypeResolutionResult {
        if (receiverExpr == null || receiverExpr.trim().isEmpty()) {
            return TypeResolutionResult.unknown()
        }

        val expr = receiverExpr.trim()

        // 1. Literal values
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return TypeResolutionResult.resolved("String")
        }
        if (expr.matches(Regex("^-?\\d+$"))) {
            return TypeResolutionResult.resolved("Int")
        }
        if (expr.matches(Regex("^-?\\d+L$"))) {
            return TypeResolutionResult.resolved("Long")
        }
        if (expr.matches(Regex("^-?\\d*\\.\\d+f?$"))) {
            return TypeResolutionResult.resolved(if (expr.endsWith("f")) "Float" else "Double")
        }
        if ("true" == expr || "false" == expr) {
            return TypeResolutionResult.resolved("Boolean")
        }
        if (expr.startsWith("listOf(") || expr.startsWith("mutableListOf(")) {
            return TypeResolutionResult.resolved("List")
        }
        if (expr.startsWith("mapOf(") || expr.startsWith("mutableMapOf(")) {
            return TypeResolutionResult.resolved("Map")
        }
        if (expr.startsWith("setOf(") || expr.startsWith("mutableSetOf(")) {
            return TypeResolutionResult.resolved("Set")
        }

        // 2. Constructor invocation (e.g. User())
        if (expr.contains("(") && expr.endsWith(")") && expr[0].isUpperCase()) {
            val className = expr.substring(0, expr.indexOf('(')).trim()
            return TypeResolutionResult.resolved(className)
        }

        // 3. Local variable or parameter lookup
        val localSymbols: List<Symbol> = localSymbolResolver.resolveLocalSymbols(source, cursorOffset, parsedFile)
        for (s in localSymbols) {
            if (expr == s.name && s.returnType.isNotEmpty()) {
                return TypeResolutionResult.resolved(s.returnType)
            }
        }

        // 4. Function invocation return type lookup (e.g. getUser())
        if (expr.endsWith(")")) {
            val funName = expr.substring(0, expr.indexOf('(')).trim()
            if (parsedFile != null) {
                for (s in parsedFile.topLevelSymbols) {
                    if (funName == s.name && s.returnType.isNotEmpty()) {
                        return TypeResolutionResult.resolved(s.returnType)
                    }
                }
            }
        }

        // 5. Check if the receiver is a known Class name (companion / static access)
        if (expr[0].isUpperCase()) {
            return TypeResolutionResult.resolved(expr)
        }

        return TypeResolutionResult.unknown()
    }
}
