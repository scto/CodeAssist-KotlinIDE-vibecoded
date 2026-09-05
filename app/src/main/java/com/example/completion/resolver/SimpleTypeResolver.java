package com.example.completion.resolver;

import com.example.completion.psi.ParsedKotlinFile;
import com.example.completion.symbol.Symbol;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic, lightweight type resolution engine for receiver expressions.
 */
public class SimpleTypeResolver {

    private final LocalSymbolResolver localSymbolResolver;

    public SimpleTypeResolver() {
        this(new LocalSymbolResolver());
    }

    public SimpleTypeResolver(LocalSymbolResolver localSymbolResolver) {
        this.localSymbolResolver = localSymbolResolver;
    }

    public TypeResolutionResult resolveReceiverType(String receiverExpr, String source, int cursorOffset, ParsedKotlinFile parsedFile) {
        if (receiverExpr == null || receiverExpr.trim().isEmpty()) {
            return TypeResolutionResult.unknown();
        }

        String expr = receiverExpr.trim();

        // 1. Literal values
        if (expr.startsWith("\"") && expr.endsWith("\"")) {
            return TypeResolutionResult.resolved("String");
        }
        if (expr.matches("^-?\\d+$")) {
            return TypeResolutionResult.resolved("Int");
        }
        if (expr.matches("^-?\\d+L$")) {
            return TypeResolutionResult.resolved("Long");
        }
        if (expr.matches("^-?\\d*\\.\\d+f?$")) {
            return TypeResolutionResult.resolved(expr.endsWith("f") ? "Float" : "Double");
        }
        if ("true".equals(expr) || "false".equals(expr)) {
            return TypeResolutionResult.resolved("Boolean");
        }
        if (expr.startsWith("listOf(") || expr.startsWith("mutableListOf(")) {
            return TypeResolutionResult.resolved("List");
        }
        if (expr.startsWith("mapOf(") || expr.startsWith("mutableMapOf(")) {
            return TypeResolutionResult.resolved("Map");
        }
        if (expr.startsWith("setOf(") || expr.startsWith("mutableSetOf(")) {
            return TypeResolutionResult.resolved("Set");
        }

        // 2. Constructor invocation (e.g. User())
        if (expr.contains("(") && expr.endsWith(")") && Character.isUpperCase(expr.charAt(0))) {
            String className = expr.substring(0, expr.indexOf('(')).trim();
            return TypeResolutionResult.resolved(className);
        }

        // 3. Local variable or parameter lookup
        List<Symbol> localSymbols = localSymbolResolver.resolveLocalSymbols(source, cursorOffset, parsedFile);
        for (Symbol s : localSymbols) {
            if (expr.equals(s.getName()) && s.getReturnType() != null && !s.getReturnType().isEmpty()) {
                return TypeResolutionResult.resolved(s.getReturnType());
            }
        }

        // 4. Function invocation return type lookup (e.g. getUser())
        if (expr.endsWith(")")) {
            String funName = expr.substring(0, expr.indexOf('(')).trim();
            if (parsedFile != null) {
                for (Symbol s : parsedFile.getTopLevelSymbols()) {
                    if (funName.equals(s.getName()) && s.getReturnType() != null && !s.getReturnType().isEmpty()) {
                        return TypeResolutionResult.resolved(s.getReturnType());
                    }
                }
            }
        }

        // 5. Check if the receiver is a known Class name (companion / static access)
        if (Character.isUpperCase(expr.charAt(0))) {
            return TypeResolutionResult.resolved(expr);
        }

        return TypeResolutionResult.unknown();
    }
}
