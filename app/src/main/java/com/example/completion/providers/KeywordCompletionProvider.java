package com.example.completion.providers;

import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionProvider;
import com.example.completion.core.CompletionRequest;
import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.psi.CompletionContextType;
import com.example.completion.psi.PsiCompletionContext;
import com.example.completion.psi.PsiContextResolver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Provides Kotlin keywords completion when appropriate for the current context.
 */
public class KeywordCompletionProvider implements CompletionProvider {

    private static final List<String> KEYWORDS = Arrays.asList(
            "class", "interface", "object", "fun", "val", "var",
            "if", "else", "when", "for", "while", "do", "return",
            "package", "import", "public", "private", "protected",
            "internal", "override", "open", "abstract", "sealed",
            "data", "enum", "companion", "suspend", "inline",
            "try", "catch", "finally", "throw", "null", "true", "false",
            "is", "as", "in", "typealias", "constructor", "init"
    );

    private final PsiContextResolver contextResolver;

    public KeywordCompletionProvider() {
        this(new PsiContextResolver());
    }

    public KeywordCompletionProvider(PsiContextResolver contextResolver) {
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "keyword-completion-provider";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean isApplicable(CompletionRequest request) {
        PsiCompletionContext ctx = contextResolver.resolveContext(request, null);
        // Do not suggest keywords during member access ("obj.cl|"), string literals, or imports
        return ctx.getContextType() != CompletionContextType.MEMBER_ACCESS &&
                ctx.getContextType() != CompletionContextType.STRING &&
                ctx.getContextType() != CompletionContextType.COMMENT &&
                ctx.getContextType() != CompletionContextType.IMPORT &&
                ctx.getContextType() != CompletionContextType.PACKAGE;
    }

    @Override
    public List<CompletionItem> complete(CompletionRequest request) {
        String prefix = request.getPrefix();
        List<CompletionItem> items = new ArrayList<>();

        for (String kw : KEYWORDS) {
            if (prefix == null || prefix.isEmpty() || kw.startsWith(prefix.toLowerCase())) {
                items.add(CompletionItem.builder(kw, SymbolKind.KEYWORD)
                        .insertText(kw + " ")
                        .detail("keyword")
                        .origin(SymbolOrigin.KEYWORD)
                        .priority(10)
                        .build());
            }
        }

        return items;
    }
}
