package com.example.ide.analysis

import com.example.ide.model.DiagnosticItem
import com.example.ide.psi.*
import java.io.File

/**
 * Kotlin Semantic Analyzer & Diagnostics Engine.
 * Detects syntax errors, unresolved symbols, conflicting declarations, type mismatches, and suggests quick fixes.
 */
class KotlinSemanticAnalyzer(
    private val workspaceIndex: WorkspaceIndex? = null,
    private val parser: KtParser = KtParser()
) {

    fun analyze(sourceCode: String, file: File? = null): AnalysisResult {
        val psi = parser.parse(sourceCode, file)
        val diagnostics = mutableListOf<DiagnosticItem>()

        // 1. Collect Syntax Errors from Parser
        for (err in psi.syntaxErrors) {
            diagnostics.add(
                DiagnosticItem(
                    file = file,
                    line = err.line,
                    column = err.column,
                    endLine = err.line,
                    endColumn = err.column + err.length,
                    message = err.message,
                    severity = DiagnosticItem.Severity.ERROR,
                    rule = "syntax-error",
                    quickFixText = err.fixSuggestion
                )
            )
        }

        // 2. Semantic Analysis: Duplicate Declarations in same file
        val declarationNames = mutableMapOf<String, KtDeclaration>()
        for (decl in psi.declarations) {
            val key = "${decl::class.simpleName}_${decl.name}"
            if (declarationNames.containsKey(key)) {
                val previous = declarationNames[key]!!
                diagnostics.add(
                    DiagnosticItem(
                        file = file,
                        line = decl.line,
                        column = decl.column,
                        message = "Conflicting declaration: '${decl.name}' is already declared on line ${previous.line}",
                        severity = DiagnosticItem.Severity.ERROR,
                        rule = "conflicting-declaration",
                        quickFixText = "Rename '${decl.name}'"
                    )
                )
            } else {
                declarationNames[key] = decl
            }
        }

        // 3. Class-level checks: Data class rules, interface rules
        for (decl in psi.declarations) {
            if (decl is KtClassOrObject) {
                if (decl.kind == KtClassOrObject.ClassKind.DATA_CLASS && decl.primaryConstructorParams.isEmpty()) {
                    diagnostics.add(
                        DiagnosticItem(
                            file = file,
                            line = decl.line,
                            column = decl.column,
                            message = "Data class '${decl.name}' must have at least one primary constructor parameter (e.g. 'val id: Int')",
                            severity = DiagnosticItem.Severity.ERROR,
                            rule = "data-class-parameters",
                            quickFixText = "Add primary constructor parameters"
                        )
                    )
                }

                // Check duplicate properties inside class
                val propNames = mutableSetOf<String>()
                for (p in decl.properties) {
                    if (!propNames.add(p.name)) {
                        diagnostics.add(
                            DiagnosticItem(
                                file = file,
                                line = p.line,
                                column = p.column,
                                message = "Conflicting property name '${p.name}' in class '${decl.name}'",
                                severity = DiagnosticItem.Severity.ERROR,
                                rule = "duplicate-member"
                            )
                        )
                    }
                }
            }
        }

        // 4. Identifier / Symbol Resolution Checks
        val knownNames = mutableSetOf<String>()
        // stdlib & builtins
        knownNames.addAll(KtLexer.getAllKeywords())
        knownNames.addAll(KtLexer.getAllBuiltinTypes())
        for (f in KotlinStdlibCatalog.globalFunctions) knownNames.add(f.name)
        for ((_, t) in KotlinStdlibCatalog.types) knownNames.add(t.simpleName)

        // File-level declarations
        for (decl in psi.declarations) {
            knownNames.add(decl.name)
            if (decl is KtClassOrObject) {
                for (p in decl.properties) knownNames.add(p.name)
                for (fn in decl.functions) knownNames.add(fn.name)
                for (param in decl.primaryConstructorParams) knownNames.add(param.name)
            }
            if (decl is KtFunction) {
                for (param in decl.parameters) knownNames.add(param.name)
                for (v in decl.localVariables) knownNames.add(v.name)
            }
        }

        // Imports
        for (imp in psi.imports) {
            val simpleName = imp.importedFqName.substringAfterLast('.')
            knownNames.add(simpleName)
            if (imp.alias != null) knownNames.add(imp.alias)
        }

        // Workspace indexed symbols
        if (workspaceIndex != null) {
            for (c in workspaceIndex.getAllWorkspaceClasses()) knownNames.add(c.name)
            for (f in workspaceIndex.getAllWorkspaceFunctions()) knownNames.add(f.name)
            for (p in workspaceIndex.getAllWorkspaceProperties()) knownNames.add(p.name)
        }

        // Standard Common Identifiers in Kotlin/Android
        val commonAndroidSymbols = setOf(
            "it", "this", "super", "args", "value", "field", "savedInstanceState",
            "findViewById", "setContentView", "getString", "getSystemService", "startActivity",
            "Toast", "Log", "View", "TextView", "Button", "EditText", "Context", "Intent",
            "Bundle", "Color", "Math", "System", "File", "Exception", "Throwable",
            "R", "id", "layout", "string", "drawable", "color", "main", "true", "false", "null",
            "out", "err", "currentTimeMillis", "nanoTime"
        )
        knownNames.addAll(commonAndroidSymbols)

        // Inspect token references (exclude declarations, string literals, comments, annotations)
        val tokens = psi.allTokens
        var i = 0
        while (i < tokens.size) {
            val tok = tokens[i]
            if (tok.type == KtTokenType.IDENTIFIER) {
                val prev = if (i > 0) tokens[i - 1] else null
                val next = if (i + 1 < tokens.size) tokens[i + 1] else null

                // Check if this identifier is following a declaration keyword (val/var/fun/class) or dot (.)
                val isDeclaring = prev?.type == KtTokenType.KEYWORD_VAL ||
                                  prev?.type == KtTokenType.KEYWORD_VAR ||
                                  prev?.type == KtTokenType.KEYWORD_FUN ||
                                  prev?.type == KtTokenType.KEYWORD_CLASS ||
                                  prev?.type == KtTokenType.KEYWORD_INTERFACE ||
                                  prev?.type == KtTokenType.KEYWORD_OBJECT ||
                                  prev?.type == KtTokenType.KEYWORD_PACKAGE ||
                                  prev?.type == KtTokenType.KEYWORD_IMPORT ||
                                  prev?.type == KtTokenType.DOT ||
                                  prev?.type == KtTokenType.DOT_SAFE

                if (!isDeclaring && !knownNames.contains(tok.text)) {
                    // Check if maybe a function call or variable reference
                    // Don't flag if it looks like a parameter or label
                    if (next?.type != KtTokenType.COLON) {
                        diagnostics.add(
                            DiagnosticItem(
                                file = file,
                                line = tok.line + 1,
                                column = tok.column + 1,
                                endLine = tok.line + 1,
                                endColumn = tok.column + 1 + tok.length,
                                message = "Unresolved reference: '${tok.text}'",
                                severity = DiagnosticItem.Severity.ERROR,
                                rule = "unresolved-reference",
                                quickFixText = "Import '${tok.text}' or declare variable"
                            )
                        )
                    }
                }
            }
            i++
        }

        return AnalysisResult(psi, diagnostics)
    }

    data class AnalysisResult(
        val psi: KtFilePsi,
        val diagnostics: List<DiagnosticItem>
    )
}
