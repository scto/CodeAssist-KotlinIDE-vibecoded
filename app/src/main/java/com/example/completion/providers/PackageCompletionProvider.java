package com.example.completion.providers;

import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionProvider;
import com.example.completion.core.CompletionRequest;
import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.index.repository.SymbolRepository;
import com.example.completion.psi.CompletionContextType;
import com.example.completion.psi.PsiCompletionContext;
import com.example.completion.psi.PsiContextResolver;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides package path completions for package directives.
 */
public class PackageCompletionProvider implements CompletionProvider {

    private final SymbolRepository symbolRepository;
    private final PsiContextResolver contextResolver;

    public PackageCompletionProvider(SymbolRepository symbolRepository, PsiContextResolver contextResolver) {
        this.symbolRepository = symbolRepository;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "package-completion-provider";
    }

    @Override
    public int getPriority() {
        return 950;
    }

    @Override
    public boolean isApplicable(CompletionRequest request) {
        PsiCompletionContext ctx = contextResolver.resolveContext(request, null);
        return ctx.getContextType() == CompletionContextType.PACKAGE;
    }

    @Override
    public List<CompletionItem> complete(CompletionRequest request) {
        PsiCompletionContext ctx = contextResolver.resolveContext(request, null);
        String prefix = ctx.getPrefix();
        List<CompletionItem> items = new ArrayList<>();

        if (symbolRepository == null) return items;

        int lastDot = prefix.lastIndexOf('.');
        for (String pkg : symbolRepository.getMatchingPackages(prefix)) {
            String label = lastDot != -1 ? pkg.substring(lastDot + 1) : pkg;
            items.add(CompletionItem.builder(label, SymbolKind.PACKAGE)
                    .insertText(label)
                    .detail(pkg)
                    .origin(SymbolOrigin.SOURCE)
                    .priority(300)
                    .build());
        }

        return items;
    }
}
