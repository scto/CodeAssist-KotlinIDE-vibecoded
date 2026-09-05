package com.example.completion.resolver;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.psi.KotlinPsiElement;
import com.example.completion.psi.KtElementKind;
import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves local variables, function parameters, and block-scoped symbols visible at the cursor offset.
 */
public class LocalSymbolResolver {

    private static final Pattern LOCAL_VAL_VAR_PATTERN = Pattern.compile("(val|var)\\s+([a-zA-Z0-9_]+)(?:\\s*:\\s*([a-zA-Z0-9_<>., ?]+))?(?:\\s*=\\s*([^\\n;]+))?");
    private static final Pattern FOR_LOOP_PATTERN = Pattern.compile("for\\s*\\(\\s*([a-zA-Z0-9_]+)\\s+in\\s+([^)]+)\\)");
    private static final Pattern CATCH_PATTERN = Pattern.compile("catch\\s*\\(\\s*([a-zA-Z0-9_]+)\\s*:\\s*([a-zA-Z0-9_<>.]+)\\)");

    public List<Symbol> resolveLocalSymbols(String source, int cursorOffset, ParsedKotlinFile parsedFile) {
        if (source == null || source.isEmpty() || cursorOffset <= 0) {
            return Collections.emptyList();
        }

        int safeOffset = Math.min(cursorOffset, source.length());
        List<Symbol> localSymbols = new ArrayList<>();

        // 1. Walk AST children if available
        if (parsedFile != null && parsedFile.getRootElement() != null) {
            KotlinPsiElement current = parsedFile.findElementAt(safeOffset);
            while (current != null) {
                if (current.getKind() == KtElementKind.FUNCTION) {
                    for (KotlinPsiElement child : current.getChildren()) {
                        if (child.getKind() == KtElementKind.PARAMETER) {
                            localSymbols.add(Symbol.builder()
                                    .name(child.getName())
                                    .kind(SymbolKind.PARAMETER)
                                    .origin(SymbolOrigin.LOCAL)
                                    .returnType(child.getType() != null ? child.getType() : "Any")
                                    .build());
                        }
                    }
                }
                current = current.getParent();
            }
        }

        // 2. Scan text lines up to cursor offset, respecting block scopes with brace tracking
        String textBeforeCursor = source.substring(0, safeOffset);
        String[] lines = textBeforeCursor.split("\n", -1);

        int openBraceCount = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                continue;
            }

            // Local val/var
            Matcher valMatcher = LOCAL_VAL_VAR_PATTERN.matcher(line);
            while (valMatcher.find()) {
                String name = valMatcher.group(2);
                String explicitType = valMatcher.group(3);
                String init = valMatcher.group(4);
                String type = explicitType != null ? explicitType.trim() : inferType(init);

                localSymbols.add(Symbol.builder()
                        .name(name)
                        .kind(SymbolKind.VARIABLE)
                        .origin(SymbolOrigin.LOCAL)
                        .returnType(type)
                        .build());
            }

            // For loop variable
            Matcher forMatcher = FOR_LOOP_PATTERN.matcher(line);
            if (forMatcher.find()) {
                String varName = forMatcher.group(1);
                localSymbols.add(Symbol.builder()
                        .name(varName)
                        .kind(SymbolKind.VARIABLE)
                        .origin(SymbolOrigin.LOCAL)
                        .returnType("Any")
                        .build());
            }

            // Catch clause variable
            Matcher catchMatcher = CATCH_PATTERN.matcher(line);
            if (catchMatcher.find()) {
                String exName = catchMatcher.group(1);
                String exType = catchMatcher.group(2);
                localSymbols.add(Symbol.builder()
                        .name(exName)
                        .kind(SymbolKind.VARIABLE)
                        .origin(SymbolOrigin.LOCAL)
                        .returnType(exType != null ? exType.trim() : "Throwable")
                        .build());
            }
        }

        return localSymbols;
    }

    private String inferType(String init) {
        if (init == null) return "Any";
        String clean = init.trim();
        if (clean.startsWith("\"") && clean.endsWith("\"")) return "String";
        if (clean.matches("^-?\\d+$")) return "Int";
        if (clean.matches("^-?\\d+L$")) return "Long";
        if (clean.matches("^-?\\d*\\.\\d+f?$")) return clean.endsWith("f") ? "Float" : "Double";
        if ("true".equals(clean) || "false".equals(clean)) return "Boolean";
        return "Any";
    }
}
