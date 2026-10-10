package com.example.completion.psi

import com.example.completion.symbol.Symbol

/**
 * Representation of an analyzed Kotlin file containing the AST/PSI root,
 * package declaration, imports, and top-level / inner declarations.
 */
class ParsedKotlinFile(
    fileName: String?,
    source: String?,
    packageName: String?,
    imports: List<String>?,
    val rootElement: KotlinPsiElement?,
    topLevelSymbols: List<Symbol>?
) {
    val fileName: String = fileName ?: "Unknown.kt"
    val source: String = source ?: ""
    val packageName: String = packageName ?: ""
    val imports: List<String> = imports?.let { ArrayList(it) } ?: emptyList()
    val topLevelSymbols: List<Symbol> = topLevelSymbols?.let { ArrayList(it) } ?: emptyList()

    fun findElementAt(offset: Int): KotlinPsiElement? {
        return rootElement?.findElementAt(offset)
    }
}
