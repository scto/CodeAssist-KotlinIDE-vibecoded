package com.example.completion.providers;

import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionProvider;
import com.example.completion.core.CompletionRequest;
import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.index.repository.SymbolRepository;
import com.example.completion.psi.CompletionContextType;
import com.example.completion.psi.KotlinPsiManager;
import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.psi.PsiCompletionContext;
import com.example.completion.psi.PsiContextResolver;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Suggests functions, properties, and symbols defined across project source files and current file.
 */
public class SourceCompletionProvider implements CompletionProvider {

    private final SymbolRepository symbolRepository;
    private final KotlinPsiManager psiManager;
    private final PsiContextResolver contextResolver;

    public SourceCompletionProvider(SymbolRepository symbolRepository, KotlinPsiManager psiManager, PsiContextResolver contextResolver) {
        this.symbolRepository = symbolRepository;
        this.psiManager = psiManager;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "source-completion-provider";
    }

    @Override
    public int getPriority() {
        return 700;
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
        List<CompletionItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String prefix = request.getPrefix();

        // 1. Current File top-level symbols
        ParsedKotlinFile parsed = psiManager.parse(request.getFilePath(), request.getContent());
        if (parsed != null) {
            for (Symbol s : parsed.getTopLevelSymbols()) {
                if (prefix == null || prefix.isEmpty() || s.getName().toLowerCase().startsWith(prefix.toLowerCase())) {
                    if (seen.add(s.getName() + ":" + s.getKind())) {
                        String insertText = s.getKind() == SymbolKind.FUNCTION ? s.getName() + "()" : s.getName();
                        String detail = s.getKind() == SymbolKind.FUNCTION ? "fun " + s.getName() + s.getParameterSignature() + ": " + s.getReturnType() : s.getReturnType();
                        items.add(CompletionItem.builder(s.getName(), s.getKind())
                                .insertText(insertText)
                                .detail(detail)
                                .type(s.getReturnType())
                                .origin(SymbolOrigin.SOURCE)
                                .priority(350)
                                .build());
                    }
                }
            }
        }

        // 2. Project workspace source symbols
        if (symbolRepository != null) {
            for (Symbol s : symbolRepository.search(prefix)) {
                if (s.getOrigin() == SymbolOrigin.SOURCE || s.getOrigin() == SymbolOrigin.BUILTIN) {
                    if (seen.add(s.getName() + ":" + s.getKind())) {
                        String insertText = s.getKind() == SymbolKind.FUNCTION ? s.getName() + "()" : s.getName();
                        String detail = s.getKind() == SymbolKind.FUNCTION ? "fun " + s.getName() + s.getParameterSignature() + ": " + s.getReturnType() : s.getReturnType();
                        items.add(CompletionItem.builder(s.getName(), s.getKind())
                                .insertText(insertText)
                                .detail(detail)
                                .type(s.getReturnType())
                                .origin(s.getOrigin())
                                .priority(250)
                                .build());
                    }
                }
            }
        }

        return items;
    }
}
