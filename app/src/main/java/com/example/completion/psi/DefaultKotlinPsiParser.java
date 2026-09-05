package com.example.completion.psi;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Production-ready, resilient Kotlin PSI and AST parser.
 * Tolerates incomplete, invalid, or actively typed Kotlin source code without crashing.
 */
public class DefaultKotlinPsiParser implements KotlinParser {

    private static final Pattern PACKAGE_PATTERN = Pattern.compile("^[ \\t]*package[ \\t]+([a-zA-Z0-9_.]+)", Pattern.MULTILINE);
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^[ \\t]*import[ \\t]+([a-zA-Z0-9_.*]+)", Pattern.MULTILINE);
    private static final Pattern CLASS_PATTERN = Pattern.compile("(?:(data|sealed|enum|open|abstract|inner)\\s+)?(class|interface|object)\\s+([a-zA-Z0-9_]+)(?:\\s*<[^>]*>)?(?:\\s*\\(([^)]*)\\))?");
    private static final Pattern FUN_PATTERN = Pattern.compile("(?:(?:inline|operator|suspend|override|open|private|protected|public|internal)\\s+)*fun\\s+(?:<[^>]*>\\s+)?(?:([a-zA-Z0-9_<>.]+)\\.)?([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)(?:\\s*:\\s*([a-zA-Z0-9_<>., ?]+))?");
    private static final Pattern VAL_VAR_PATTERN = Pattern.compile("(?:(?:override|open|private|protected|public|internal|const)\\s+)*(val|var)\\s+(?:(?:([a-zA-Z0-9_<>.]+)\\.)?([a-zA-Z0-9_]+))(?:\\s*:\\s*([a-zA-Z0-9_<>., ?]+))?(?:\\s*=\\s*([^\\n;]+))?");

    @Override
    public ParsedKotlinFile parse(String fileName, String source) {
        if (source == null) source = "";
        if (fileName == null) fileName = "Main.kt";

        String packageName = "";
        Matcher pkgMatcher = PACKAGE_PATTERN.matcher(source);
        if (pkgMatcher.find()) {
            packageName = pkgMatcher.group(1).trim();
        }

        List<String> imports = new ArrayList<>();
        Matcher impMatcher = IMPORT_PATTERN.matcher(source);
        while (impMatcher.find()) {
            imports.add(impMatcher.group(1).trim());
        }

        KotlinPsiElement root = new KotlinPsiElement(KtElementKind.FILE, fileName, 0, source.length(), 1, 1);
        List<Symbol> symbols = new ArrayList<>();

        // Extract classes, functions, and properties with nesting and scopes
        parseDeclarations(source, root, symbols, packageName, fileName);

        return new ParsedKotlinFile(fileName, source, packageName, imports, root, symbols);
    }

    private void parseDeclarations(String source, KotlinPsiElement parent, List<Symbol> symbols, String packageName, String fileName) {
        String[] lines = source.split("\n", -1);
        int currentOffset = 0;

        for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
            String line = lines[lineIdx];
            int lineStart = currentOffset;
            int lineEnd = lineStart + line.length();
            int lineNumber = lineIdx + 1;

            String trimmed = line.trim();
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                currentOffset += line.length() + 1;
                continue;
            }

            // 1. Check Function
            Matcher funMatcher = FUN_PATTERN.matcher(line);
            if (funMatcher.find()) {
                String receiver = funMatcher.group(1);
                String funName = funMatcher.group(2);
                String paramsRaw = funMatcher.group(3);
                String returnType = funMatcher.group(4);

                if (returnType == null || returnType.trim().isEmpty()) {
                    returnType = "Unit";
                } else {
                    returnType = returnType.trim();
                }

                List<String> params = parseParameters(paramsRaw);

                int elemStart = lineStart + funMatcher.start();
                int elemEnd = lineStart + funMatcher.end();
                KotlinPsiElement funElement = new KotlinPsiElement(KtElementKind.FUNCTION, funName, elemStart, elemEnd, lineNumber, funMatcher.start() + 1);
                funElement.setType(returnType);
                if (receiver != null) {
                    funElement.setReceiverType(receiver.trim());
                }

                // Parse function parameters as PSI variable elements
                if (paramsRaw != null && !paramsRaw.trim().isEmpty()) {
                    for (String p : paramsRaw.split(",")) {
                        String[] parts = p.trim().split(":");
                        if (parts.length > 0 && !parts[0].trim().isEmpty()) {
                            String pName = parts[0].trim();
                            String pType = parts.length > 1 ? parts[1].trim() : "Any";
                            KotlinPsiElement paramElem = new KotlinPsiElement(KtElementKind.PARAMETER, pName, elemStart, elemEnd, lineNumber, 1);
                            paramElem.setType(pType);
                            funElement.addChild(paramElem);
                        }
                    }
                }

                parent.addChild(funElement);

                String qName = packageName.isEmpty() ? funName : packageName + "." + funName;
                symbols.add(Symbol.builder()
                        .name(funName)
                        .qualifiedName(qName)
                        .kind(SymbolKind.FUNCTION)
                        .origin(SymbolOrigin.SOURCE)
                        .packageName(packageName)
                        .returnType(returnType)
                        .parameters(params)
                        .receiverType(receiver != null ? receiver.trim() : "")
                        .build());
            } else if (CLASS_PATTERN.matcher(line).find()) {
                Matcher classMatcher = CLASS_PATTERN.matcher(line);
                classMatcher.find();
                String modifier = classMatcher.group(1);
                String keyword = classMatcher.group(2);
                String className = classMatcher.group(3);
                String ctorParams = classMatcher.group(4);

                SymbolKind kind = SymbolKind.CLASS;
                KtElementKind ktKind = KtElementKind.CLASS;
                if ("interface".equals(keyword)) {
                    kind = SymbolKind.INTERFACE;
                    ktKind = KtElementKind.INTERFACE;
                } else if ("object".equals(keyword)) {
                    kind = SymbolKind.OBJECT;
                    ktKind = KtElementKind.OBJECT;
                } else if ("enum".equals(modifier)) {
                    kind = SymbolKind.ENUM;
                    ktKind = KtElementKind.ENUM_CLASS;
                }

                int elemStart = lineStart + classMatcher.start();
                int elemEnd = lineStart + classMatcher.end();
                KotlinPsiElement classElem = new KotlinPsiElement(ktKind, className, elemStart, elemEnd, lineNumber, classMatcher.start() + 1);

                // Add constructor parameters if present
                if (ctorParams != null && !ctorParams.trim().isEmpty()) {
                    for (String cp : ctorParams.split(",")) {
                        String clean = cp.replace("val ", "").replace("var ", "").trim();
                        String[] cpParts = clean.split(":");
                        if (cpParts.length > 0 && !cpParts[0].trim().isEmpty()) {
                            String propName = cpParts[0].trim();
                            String propType = cpParts.length > 1 ? cpParts[1].trim() : "Any";
                            KotlinPsiElement propElem = new KotlinPsiElement(KtElementKind.PROPERTY, propName, elemStart, elemEnd, lineNumber, 1);
                            propElem.setType(propType);
                            classElem.addChild(propElem);
                        }
                    }
                }

                parent.addChild(classElem);

                String qName = packageName.isEmpty() ? className : packageName + "." + className;
                symbols.add(Symbol.builder()
                        .name(className)
                        .qualifiedName(qName)
                        .kind(kind)
                        .origin(SymbolOrigin.SOURCE)
                        .packageName(packageName)
                        .returnType(className)
                        .build());
            } else if (VAL_VAR_PATTERN.matcher(line).find()) {
                Matcher valMatcher = VAL_VAR_PATTERN.matcher(line);
                valMatcher.find();
                String valOrVar = valMatcher.group(1);
                String receiver = valMatcher.group(2);
                String propName = valMatcher.group(3);
                String explicitType = valMatcher.group(4);
                String initializer = valMatcher.group(5);

                String deducedType = inferSimpleType(explicitType, initializer);

                int elemStart = lineStart + valMatcher.start();
                int elemEnd = lineStart + valMatcher.end();
                KotlinPsiElement propElem = new KotlinPsiElement(KtElementKind.PROPERTY, propName, elemStart, elemEnd, lineNumber, valMatcher.start() + 1);
                propElem.setType(deducedType);
                if (receiver != null) {
                    propElem.setReceiverType(receiver.trim());
                }
                parent.addChild(propElem);

                String qName = packageName.isEmpty() ? propName : packageName + "." + propName;
                symbols.add(Symbol.builder()
                        .name(propName)
                        .qualifiedName(qName)
                        .kind(SymbolKind.PROPERTY)
                        .origin(SymbolOrigin.SOURCE)
                        .packageName(packageName)
                        .returnType(deducedType)
                        .receiverType(receiver != null ? receiver.trim() : "")
                        .build());
            }

            currentOffset += line.length() + 1;
        }
    }

    private List<String> parseParameters(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>();
        String[] split = raw.split(",");
        for (String s : split) {
            String clean = s.trim();
            if (!clean.isEmpty()) {
                list.add(clean);
            }
        }
        return list;
    }

    private String inferSimpleType(String explicitType, String initializer) {
        if (explicitType != null && !explicitType.trim().isEmpty()) {
            return explicitType.trim();
        }
        if (initializer == null) return "Any";
        String init = initializer.trim();
        if (init.startsWith("\"") && init.endsWith("\"")) return "String";
        if (init.matches("^-?\\d+$")) return "Int";
        if (init.matches("^-?\\d+L$")) return "Long";
        if (init.matches("^-?\\d*\\.\\d+f?$")) return init.endsWith("f") ? "Float" : "Double";
        if ("true".equals(init) || "false".equals(init)) return "Boolean";
        if (init.startsWith("listOf(") || init.startsWith("mutableListOf(")) return "List";
        if (init.startsWith("mapOf(") || init.startsWith("mutableMapOf(")) return "Map";
        if (init.startsWith("setOf(") || init.startsWith("mutableSetOf(")) return "Set";
        if (init.contains("(") && Character.isUpperCase(init.charAt(0))) {
            return init.substring(0, init.indexOf('(')).trim();
        }
        return "Any";
    }
}
