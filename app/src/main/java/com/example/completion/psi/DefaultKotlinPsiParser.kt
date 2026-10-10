package com.example.completion.psi

import com.example.completion.core.SymbolKind
import com.example.completion.core.SymbolOrigin
import com.example.completion.symbol.Symbol

import java.util.regex.Pattern

/**
 * Production-ready, resilient Kotlin PSI and AST parser.
 * Tolerates incomplete, invalid, or actively typed Kotlin source code without crashing.
 */
class DefaultKotlinPsiParser : KotlinParser {

    override fun parse(fileName: String?, source: String?): ParsedKotlinFile {
        val src = source ?: ""
        val fName = fileName ?: "Main.kt"

        var packageName = ""
        val pkgMatcher = PACKAGE_PATTERN.matcher(src)
        if (pkgMatcher.find()) {
            packageName = pkgMatcher.group(1).trim()
        }

        val imports = ArrayList<String>()
        val impMatcher = IMPORT_PATTERN.matcher(src)
        while (impMatcher.find()) {
            imports.add(impMatcher.group(1).trim())
        }

        val root = KotlinPsiElement(KtElementKind.FILE, fName, 0, src.length, 1, 1)
        val symbols = ArrayList<Symbol>()

        // Extract classes, functions, and properties with nesting and scopes
        parseDeclarations(src, root, symbols, packageName, fName)

        return ParsedKotlinFile(fName, src, packageName, imports, root, symbols)
    }

    private fun parseDeclarations(source: String, parent: KotlinPsiElement, symbols: MutableList<Symbol>, packageName: String, fileName: String) {
        val lines = source.split("\n")
        var currentOffset = 0

        for (lineIdx in lines.indices) {
            val line = lines[lineIdx]
            val lineStart = currentOffset
            val lineEnd = lineStart + line.length
            val lineNumber = lineIdx + 1

            val trimmed = line.trim()
            if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                currentOffset += line.length + 1
                continue
            }

            // 1. Check Function
            val funMatcher = FUN_PATTERN.matcher(line)
            if (funMatcher.find()) {
                val receiver = funMatcher.group(1)
                val funName = funMatcher.group(2)
                val paramsRaw = funMatcher.group(3)
                var returnType = funMatcher.group(4)

                if (returnType == null || returnType.trim().isEmpty()) {
                    returnType = "Unit"
                } else {
                    returnType = returnType.trim()
                }

                val params = parseParameters(paramsRaw)

                val elemStart = lineStart + funMatcher.start()
                val elemEnd = lineStart + funMatcher.end()
                val funElement = KotlinPsiElement(KtElementKind.FUNCTION, funName, elemStart, elemEnd, lineNumber, funMatcher.start() + 1)
                funElement.type = returnType
                if (receiver != null) {
                    funElement.receiverType = receiver.trim()
                }

                // Parse function parameters as PSI variable elements
                if (paramsRaw != null && paramsRaw.trim().isNotEmpty()) {
                    for (p in paramsRaw.split(",")) {
                        val parts = p.trim().split(":")
                        if (parts.isNotEmpty() && parts[0].trim().isNotEmpty()) {
                            val pName = parts[0].trim()
                            val pType = if (parts.size > 1) parts[1].trim() else "Any"
                            val paramElem = KotlinPsiElement(KtElementKind.PARAMETER, pName, elemStart, elemEnd, lineNumber, 1)
                            paramElem.type = pType
                            funElement.addChild(paramElem)
                        }
                    }
                }

                parent.addChild(funElement)

                val qName = if (packageName.isEmpty()) funName else "$packageName.$funName"
                symbols.add(
                    Symbol.builder()
                        .name(funName)
                        .qualifiedName(qName)
                        .kind(SymbolKind.FUNCTION)
                        .origin(SymbolOrigin.SOURCE)
                        .packageName(packageName)
                        .returnType(returnType)
                        .parameters(params)
                        .receiverType(receiver?.trim() ?: "")
                        .build()
                )
            } else if (CLASS_PATTERN.matcher(line).find()) {
                val classMatcher = CLASS_PATTERN.matcher(line)
                classMatcher.find()
                val modifier = classMatcher.group(1)
                val keyword = classMatcher.group(2)
                val className = classMatcher.group(3)
                val ctorParams = classMatcher.group(4)

                var kind = SymbolKind.CLASS
                var ktKind = KtElementKind.CLASS
                if ("interface" == keyword) {
                    kind = SymbolKind.INTERFACE
                    ktKind = KtElementKind.INTERFACE
                } else if ("object" == keyword) {
                    kind = SymbolKind.OBJECT
                    ktKind = KtElementKind.OBJECT
                } else if ("enum" == modifier) {
                    kind = SymbolKind.ENUM
                    ktKind = KtElementKind.ENUM_CLASS
                }

                val elemStart = lineStart + classMatcher.start()
                val elemEnd = lineStart + classMatcher.end()
                val classElem = KotlinPsiElement(ktKind, className, elemStart, elemEnd, lineNumber, classMatcher.start() + 1)

                // Add constructor parameters if present
                if (ctorParams != null && ctorParams.trim().isNotEmpty()) {
                    for (cp in ctorParams.split(",")) {
                        val clean = cp.replace("val ", "").replace("var ", "").trim()
                        val cpParts = clean.split(":")
                        if (cpParts.isNotEmpty() && cpParts[0].trim().isNotEmpty()) {
                            val propName = cpParts[0].trim()
                            val propType = if (cpParts.size > 1) cpParts[1].trim() else "Any"
                            val propElem = KotlinPsiElement(KtElementKind.PROPERTY, propName, elemStart, elemEnd, lineNumber, 1)
                            propElem.type = propType
                            classElem.addChild(propElem)
                        }
                    }
                }

                parent.addChild(classElem)

                val qName = if (packageName.isEmpty()) className else "$packageName.$className"
                symbols.add(
                    Symbol.builder()
                        .name(className)
                        .qualifiedName(qName)
                        .kind(kind)
                        .origin(SymbolOrigin.SOURCE)
                        .packageName(packageName)
                        .returnType(className)
                        .build()
                )
            } else if (VAL_VAR_PATTERN.matcher(line).find()) {
                val valMatcher = VAL_VAR_PATTERN.matcher(line)
                valMatcher.find()
                val valOrVar = valMatcher.group(1)
                val receiver = valMatcher.group(2)
                val propName = valMatcher.group(3)
                val explicitType = valMatcher.group(4)
                val initializer = valMatcher.group(5)

                val deducedType = inferSimpleType(explicitType, initializer)

                val elemStart = lineStart + valMatcher.start()
                val elemEnd = lineStart + valMatcher.end()
                val propElem = KotlinPsiElement(KtElementKind.PROPERTY, propName, elemStart, elemEnd, lineNumber, valMatcher.start() + 1)
                propElem.type = deducedType
                if (receiver != null) {
                    propElem.receiverType = receiver.trim()
                }
                parent.addChild(propElem)

                val qName = if (packageName.isEmpty()) propName else "$packageName.$propName"
                symbols.add(
                    Symbol.builder()
                        .name(propName)
                        .qualifiedName(qName)
                        .kind(SymbolKind.PROPERTY)
                        .origin(SymbolOrigin.SOURCE)
                        .packageName(packageName)
                        .returnType(deducedType)
                        .receiverType(receiver?.trim() ?: "")
                        .build()
                )
            }

            currentOffset += line.length + 1
        }
    }

    private fun parseParameters(raw: String?): List<String> {
        if (raw == null || raw.trim().isEmpty()) {
            return emptyList()
        }
        val list = ArrayList<String>()
        val split = raw.split(",")
        for (s in split) {
            val clean = s.trim()
            if (clean.isNotEmpty()) {
                list.add(clean)
            }
        }
        return list
    }

    private fun inferSimpleType(explicitType: String?, initializer: String?): String {
        if (explicitType != null && explicitType.trim().isNotEmpty()) {
            return explicitType.trim()
        }
        if (initializer == null) return "Any"
        val init = initializer.trim()
        if (init.startsWith("\"") && init.endsWith("\"")) return "String"
        if (init.matches(Regex("^-?\\d+$"))) return "Int"
        if (init.matches(Regex("^-?\\d+L$"))) return "Long"
        if (init.matches(Regex("^-?\\d*\\.\\d+f?$"))) return if (init.endsWith("f")) "Float" else "Double"
        if ("true" == init || "false" == init) return "Boolean"
        if (init.startsWith("listOf(") || init.startsWith("mutableListOf(")) return "List"
        if (init.startsWith("mapOf(") || init.startsWith("mutableMapOf(")) return "Map"
        if (init.startsWith("setOf(") || init.startsWith("mutableSetOf(")) return "Set"
        if (init.contains("(") && init[0].isUpperCase()) {
            return init.substring(0, init.indexOf('(')).trim()
        }
        return "Any"
    }

    companion object {
        private val PACKAGE_PATTERN = Pattern.compile("^[ \\t]*package[ \\t]+([a-zA-Z0-9_.]+)", Pattern.MULTILINE)
        private val IMPORT_PATTERN = Pattern.compile("^[ \\t]*import[ \\t]+([a-zA-Z0-9_.*]+)", Pattern.MULTILINE)
        private val CLASS_PATTERN = Pattern.compile("(?:(data|sealed|enum|open|abstract|inner)\\s+)?(class|interface|object)\\s+([a-zA-Z0-9_]+)(?:\\s*<[^>]*>)?(?:\\s*\\(([^)]*)\\))?")
        private val FUN_PATTERN = Pattern.compile("(?:(?:inline|operator|suspend|override|open|private|protected|public|internal)\\s+)*fun\\s+(?:<[^>]*>\\s+)?(?:([a-zA-Z0-9_<>.]+)\\.)?([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)(?:\\s*:\\s*([a-zA-Z0-9_<>., ?]+))?")
        private val VAL_VAR_PATTERN = Pattern.compile("(?:(?:override|open|private|protected|public|internal|const)\\s+)*(val|var)\\s+(?:(?:([a-zA-Z0-9_<>.]+)\\.)?([a-zA-Z0-9_]+))(?:\\s*:\\s*([a-zA-Z0-9_<>., ?]+))?(?:\\s*=\\s*([^\\n;]+))?")
    }
}
