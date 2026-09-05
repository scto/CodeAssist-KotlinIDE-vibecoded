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
 * Provides completion for import statements (subpackages and class names).
 */
public class ImportCompletionProvider implements CompletionProvider {

    private final SymbolRepository symbolRepository;
    private final PsiContextResolver contextResolver;

    public ImportCompletionProvider(SymbolRepository symbolRepository, PsiContextResolver contextResolver) {
        this.symbolRepository = symbolRepository;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "import-completion-provider";
    }

    @Override
    public int getPriority() {
        return 950;
    }

    @Override
    public boolean isApplicable(CompletionRequest request) {
        PsiCompletionContext ctx = contextResolver.resolveContext(request, null);
        return ctx.getContextType() == CompletionContextType.IMPORT;
    }

    @Override
    public List<CompletionItem> complete(CompletionRequest request) {
        PsiCompletionContext ctx = contextResolver.resolveContext(request, null);
        String prefix = ctx.getPrefix();
        List<CompletionItem> items = new ArrayList<>();

        if (symbolRepository == null) return items;

        int lastDot = prefix.lastIndexOf('.');
        String parentPkg = lastDot != -1 ? prefix.substring(0, lastDot) : "";
        String subPrefix = lastDot != -1 ? prefix.substring(lastDot + 1) : prefix;

        // 1. Suggest matching subpackages
        for (String pkg : symbolRepository.getMatchingPackages(prefix)) {
            String label = lastDot != -1 ? pkg.substring(lastDot + 1) : pkg;
            items.add(CompletionItem.builder(label, SymbolKind.PACKAGE)
                    .insertText(label)
                    .detail(pkg)
                    .origin(SymbolOrigin.JAR)
                    .priority(300)
                    .build());
        }

        // 2. Suggest classes inside the package
        if (!parentPkg.isEmpty()) {
            for (String cls : symbolRepository.getClassesInPackage(parentPkg, subPrefix)) {
                items.add(CompletionItem.builder(cls, SymbolKind.CLASS)
                        .insertText(cls)
                        .detail(parentPkg + "." + cls)
                        .origin(SymbolOrigin.JAR)
                        .priority(400)
                        .build());
            }
        }

        return items;
    }
}
