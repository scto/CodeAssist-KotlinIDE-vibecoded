package com.example.completion.resolver;

import com.example.completion.index.repository.SymbolRepository;
import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves members, methods, fields, and extension functions for dot-qualified receiver expressions.
 */
public class MemberResolver {

    private final SimpleTypeResolver typeResolver;
    private final SymbolRepository symbolRepository;

    public MemberResolver(SimpleTypeResolver typeResolver, SymbolRepository symbolRepository) {
        this.typeResolver = typeResolver;
        this.symbolRepository = symbolRepository;
    }

    public List<Symbol> resolveMembers(String receiverExpr, String source, int cursorOffset, ParsedKotlinFile parsedFile) {
        if (receiverExpr == null || receiverExpr.trim().isEmpty() || symbolRepository == null) {
            return Collections.emptyList();
        }

        TypeResolutionResult typeResult = typeResolver.resolveReceiverType(receiverExpr, source, cursorOffset, parsedFile);
        String typeName = typeResult.getTypeName();

        List<Symbol> members = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        // 1. Direct members for type
        for (Symbol s : symbolRepository.findMembers(typeName)) {
            if (seen.add(s.getName() + ":" + s.getKind())) {
                members.add(s);
            }
        }

        // 2. Extension functions matching receiver type
        for (Symbol ext : symbolRepository.findExtensionFunctions(typeName)) {
            if (seen.add(ext.getName() + ":" + ext.getKind())) {
                members.add(ext);
            }
        }

        // 3. Fallback Any members
        if (!typeName.equals("Any")) {
            for (Symbol anyMember : symbolRepository.findMembers("Any")) {
                if (seen.add(anyMember.getName() + ":" + anyMember.getKind())) {
                    members.add(anyMember);
                }
            }
        }

        return members;
    }
}
