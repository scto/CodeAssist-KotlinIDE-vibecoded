package com.example.completion.providers;

import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionProvider;
import com.example.completion.core.CompletionRequest;
import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.index.jar.JarClassSymbol;
import com.example.completion.index.repository.SymbolRepository;
import com.example.completion.psi.CompletionContextType;
import com.example.completion.psi.PsiCompletionContext;
import com.example.completion.psi.PsiContextResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Suggests class, interface, and object types with full package details and auto-import hints.
 */
public class ClassCompletionProvider implements CompletionProvider {

    private final SymbolRepository symbolRepository;
    private final PsiContextResolver contextResolver;

    public ClassCompletionProvider(SymbolRepository symbolRepository, PsiContextResolver contextResolver) {
        this.symbolRepository = symbolRepository;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "class-completion-provider";
    }

    @Override
    public int getPriority() {
        return 600;
    }

    @Override
    public boolean isApplicable(CompletionRequest request) {
        PsiCompletionContext ctx = contextResolver.resolveContext(request, null);
        return ctx.getContextType() != CompletionContextType.MEMBER_ACCESS &&
                ctx.getContextType() != CompletionContextType.STRING &&
                ctx.getContextType() != CompletionContextType.COMMENT;
    }

    @Override
    public List<CompletionItem> complete(CompletionRequest request) {
        List<CompletionItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String prefix = request.getPrefix();

        if (symbolRepository != null) {
            for (JarClassSymbol jc : symbolRepository.searchClasses(prefix)) {
                if (seen.add(jc.getQualifiedName())) {
                    items.add(CompletionItem.builder(jc.getName(), jc.getKind())
                            .insertText(jc.getName())
                            .detail(jc.getQualifiedName())
                            .origin(SymbolOrigin.JAR)
                            .importToInsert(jc.getQualifiedName())
                            .priority(200)
                            .build());
                }
            }
        }

        return items;
    }
}
