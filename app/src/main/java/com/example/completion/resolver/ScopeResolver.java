package com.example.completion.resolver;

import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.List;

/**
 * High-level scope resolver that combines local symbols and top-level file symbols.
 */
public class ScopeResolver {

    private final LocalSymbolResolver localSymbolResolver;

    public ScopeResolver() {
        this(new LocalSymbolResolver());
    }

    public ScopeResolver(LocalSymbolResolver localSymbolResolver) {
        this.localSymbolResolver = localSymbolResolver;
    }

    public List<Symbol> resolveVisibleSymbols(String source, int cursorOffset, ParsedKotlinFile parsedFile) {
        List<Symbol> visible = new ArrayList<>();

        // 1. Local variables and parameters (highest precedence)
        visible.addAll(localSymbolResolver.resolveLocalSymbols(source, cursorOffset, parsedFile));

        // 2. Current file top-level declarations
        if (parsedFile != null) {
            visible.addAll(parsedFile.getTopLevelSymbols());
        }

        return visible;
    }
}
