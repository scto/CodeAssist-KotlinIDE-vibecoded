package com.example.completion.providers;

import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionProvider;
import com.example.completion.core.CompletionRequest;
import com.example.completion.core.SymbolKind;
import com.example.completion.psi.CompletionContextType;
import com.example.completion.psi.KotlinPsiManager;
import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.psi.PsiCompletionContext;
import com.example.completion.psi.PsiContextResolver;
import com.example.completion.resolver.MemberResolver;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles dot-qualified member completion (properties, methods, extension functions).
 */
public class MemberCompletionProvider implements CompletionProvider {

    private final MemberResolver memberResolver;
    private final KotlinPsiManager psiManager;
    private final PsiContextResolver contextResolver;

    public MemberCompletionProvider(MemberResolver memberResolver, KotlinPsiManager psiManager, PsiContextResolver contextResolver) {
        this.memberResolver = memberResolver;
        this.psiManager = psiManager;
        this.contextResolver = contextResolver;
    }

    @Override
    public String getId() {
        return "member-completion-provider";
    }

    @Override
    public int getPriority() {
        return 1000; // Top priority for member access
    }

    @Override
    public boolean isApplicable(CompletionRequest request) {
        PsiCompletionContext ctx = contextResolver.resolveContext(request, null);
        return ctx.getContextType() == CompletionContextType.MEMBER_ACCESS;
    }

    @Override
    public List<CompletionItem> complete(CompletionRequest request) {
        ParsedKotlinFile parsed = psiManager.parse(request.getFilePath(), request.getContent());
        PsiCompletionContext ctx = contextResolver.resolveContext(request, parsed);

        String receiver = ctx.getReceiver();
        String memberPrefix = ctx.getPrefix();

        List<Symbol> members = memberResolver.resolveMembers(receiver, request.getContent(), request.getCursorPosition(), parsed);
        List<CompletionItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Symbol m : members) {
            if (memberPrefix == null || memberPrefix.isEmpty() || m.getName().toLowerCase().startsWith(memberPrefix.toLowerCase())) {
                if (seen.add(m.getName() + ":" + m.getKind())) {
                    String insertText = m.getKind() == SymbolKind.FUNCTION ? m.getName() + "()" : m.getName();
                    String detail = m.getKind() == SymbolKind.FUNCTION ? "fun " + m.getName() + m.getParameterSignature() + ": " + m.getReturnType() : m.getReturnType();

                    items.add(CompletionItem.builder(m.getName(), m.getKind())
                            .insertText(insertText)
                            .detail(detail)
                            .type(m.getReturnType())
                            .origin(m.getOrigin())
                            .priority(600)
                            .build());
                }
            }
        }

        return items;
    }
}
