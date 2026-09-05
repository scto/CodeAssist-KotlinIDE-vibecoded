package com.example.completion.providers;

import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionProvider;
import com.example.completion.core.CompletionRequest;
import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.psi.CompletionContextType;
import com.example.completion.psi.KotlinPsiManager;
import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.psi.PsiCompletionContext;
import com.example.completion.psi.PsiContextResolver;
import com.example.completion.resolver.LocalSymbolResolver;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Suggests in-scope local variables, parameters, and expressions visible at the cursor.
 */
public class LocalCompletionProvider implements CompletionProvider {

    private final LocalSymbolResolver localSymbolResolver;
    private final KotlinPsiManager psiManager;
    private final PsiContextResolver contextResolver;

    public LocalCompletionProvider(LocalSymbolResolver localSymbolResolver, KotlinPsiManager psiManager, PsiContextResolver contextResolver) {
        this.localSymbolResolver = localSymbolResolver;
        this.psiManager = psiManager;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "local-completion-provider";
    }

    @Override
    public int getPriority() {
        return 900; // High priority
    }

    @Override
    public boolean isApplicable(CompletionRequest request) {
        PsiCompletionContext ctx = contextResolver.resolveContext(request, null);
        return ctx.getContextType() != CompletionContextType.MEMBER_ACCESS &&
                ctx.getContextType() != CompletionContextType.IMPORT &&
                ctx.getContextType() != CompletionContextType.PACKAGE &&
                ctx.getContextType() != CompletionContextType.STRING &&
                ctx.getContextType() != CompletionContextType.COMMENT;
    }

    @Override
    public List<CompletionItem> complete(CompletionRequest request) {
        ParsedKotlinFile parsed = psiManager.parse(request.getFilePath(), request.getContent());
        List<Symbol> localSymbols = localSymbolResolver.resolveLocalSymbols(request.getContent(), request.getCursorPosition(), parsed);

        List<CompletionItem> items = new ArrayList<>();
        String prefix = request.getPrefix();

        for (Symbol s : localSymbols) {
            if (prefix == null || prefix.isEmpty() || s.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                items.add(CompletionItem.builder(s.getName(), s.getKind())
                        .insertText(s.getName())
                        .detail(s.getKind() == SymbolKind.PARAMETER ? "param: " + s.getReturnType() : "val/var: " + s.getReturnType())
                        .type(s.getReturnType())
                        .origin(SymbolOrigin.LOCAL)
                        .priority(500)
                        .build());
            }
        }

        return items;
    }
}
