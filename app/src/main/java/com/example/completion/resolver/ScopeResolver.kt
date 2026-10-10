package com.example.completion.resolver

import com.example.completion.psi.ParsedKotlinFile
import com.example.completion.symbol.Symbol

/**
 * High-level scope resolver that combines local symbols and top-level file symbols.
 */
class ScopeResolver @JvmOverloads constructor(
    private val localSymbolResolver: LocalSymbolResolver = LocalSymbolResolver()
) {

    fun resolveVisibleSymbols(source: String, cursorOffset: Int, parsedFile: ParsedKotlinFile?): List<Symbol> {
        val visible = ArrayList<Symbol>()

        // 1. Local variables and parameters (highest precedence)
        visible.addAll(localSymbolResolver.resolveLocalSymbols(source, cursorOffset, parsedFile))

        // 2. Current file top-level declarations
        if (parsedFile != null) {
            visible.addAll(parsedFile.topLevelSymbols)
        }

        return visible
    }
}
