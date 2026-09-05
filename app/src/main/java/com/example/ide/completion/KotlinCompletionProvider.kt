package com.example.ide.completion

import com.example.ide.analysis.KotlinStdlibCatalog
import com.example.ide.analysis.WorkspaceIndex
import com.example.ide.psi.KtLexer
import com.example.ide.psi.KtParser
import com.example.ide.psi.KtTokenType
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference

/**
 * Intelligent Kotlin Code Completion Provider for Sora Editor.
 */
class KotlinCompletionProvider(
    private val workspaceIndex: WorkspaceIndex? = null,
    private val parser: KtParser = KtParser()
) {

    fun complete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher
    ) {
        val lineIndex = position.line
        val columnIndex = position.column
        if (lineIndex < 0 || lineIndex >= content.lineCount) return

        val lineText = content.getLine(lineIndex).toString()
        val textBeforeCursor = if (columnIndex <= lineText.length) lineText.substring(0, columnIndex) else lineText
        val prefix = extractPrefix(textBeforeCursor)

        // 1. Check for Dot completion (e.g. `obj.`)
        val isDotAccess = textBeforeCursor.trimEnd().endsWith(".") || textBeforeCursor.trimEnd().endsWith("?.")
        if (isDotAccess) {
            val expressionBeforeDot = extractExpressionBeforeDot(textBeforeCursor)
            provideDotCompletions(expressionBeforeDot, prefix, publisher)
            return
        }

        // 2. Check for Annotation completion (e.g. `@`)
        if (textBeforeCursor.trimEnd().startsWith("@") || prefix.startsWith("@")) {
            provideAnnotationCompletions(prefix.removePrefix("@"), publisher)
            return
        }

        // 3. Normal Scope / Keyword / Identifier / Snippet completion
        provideScopeCompletions(prefix, content.toString(), publisher)
    }

    private fun extractPrefix(textBeforeCursor: String): String {
        var i = textBeforeCursor.length - 1
        while (i >= 0 && (textBeforeCursor[i].isLetterOrDigit() || textBeforeCursor[i] == '_' || textBeforeCursor[i] == '@')) {
            i--
        }
        return textBeforeCursor.substring(i + 1)
    }

    private fun extractExpressionBeforeDot(textBeforeCursor: String): String {
        val trimmed = textBeforeCursor.trimEnd()
        val dotIdx = if (trimmed.endsWith("?.")) trimmed.lastIndexOf("?.") else trimmed.lastIndexOf('.')
        if (dotIdx <= 0) return ""

        var i = dotIdx - 1
        while (i >= 0 && (trimmed[i].isLetterOrDigit() || trimmed[i] == '_' || trimmed[i] == ')' || trimmed[i] == '\"')) {
            i--
        }
        return trimmed.substring(i + 1, dotIdx).trim()
    }

    private fun provideDotCompletions(expression: String, prefix: String, publisher: CompletionPublisher) {
        // Infer type from expression
        val inferredType = when {
            expression.startsWith("\"") && expression.endsWith("\"") -> "String"
            expression.equals("listOf", ignoreCase = true) || expression.contains("List") -> "List"
            expression.equals("mutableListOf", ignoreCase = true) -> "MutableList"
            expression.equals("mapOf", ignoreCase = true) || expression.contains("Map") -> "Map"
            expression.toIntOrNull() != null -> "Int"
            expression.equals("Toast", ignoreCase = false) -> "Toast"
            expression.equals("Log", ignoreCase = false) -> "Log"
            expression.contains("view", ignoreCase = true) || expression.contains("btn", ignoreCase = true) -> "View"
            expression.contains("activity", ignoreCase = true) || expression.equals("this", ignoreCase = true) -> "AppCompatActivity"
            else -> {
                // Check if known type or class in workspace
                val match = KotlinStdlibCatalog.findType(expression)
                match?.simpleName ?: "Any"
            }
        }

        val members = KotlinStdlibCatalog.getMembersForType(inferredType)
        for (m in members) {
            if (prefix.isEmpty() || m.name.contains(prefix, ignoreCase = true)) {
                val kind = if (m.isMethod) "Method" else "Property"
                val item = SimpleCompletionItem(
                    m.name,
                    "${m.signature} ($kind)",
                    prefix.length,
                    m.snippet
                )
                publisher.addItem(item)
            }
        }
    }

    private fun provideAnnotationCompletions(prefix: String, publisher: CompletionPublisher) {
        val annotations = listOf(
            "Composable" to "Declares a Composable UI function",
            "JvmStatic" to "Specifies that an additional static method should be generated",
            "JvmOverloads" to "Instructs the Kotlin compiler to generate overloads",
            "OptIn" to "Allows use of experimental API",
            "Serializable" to "Marks a class as serializable",
            "Deprecated" to "Marks the declaration as deprecated",
            "Suppress" to "Suppresses given compiler warnings",
            "Synchronized" to "Marks JVM method as synchronized",
            "Volatile" to "Marks JVM backing field as volatile",
            "Throws" to "Declares exceptions thrown by a function"
        )
        for ((ann, doc) in annotations) {
            if (prefix.isEmpty() || ann.contains(prefix, ignoreCase = true)) {
                publisher.addItem(
                    SimpleCompletionItem("@$ann", "@$ann - $doc", prefix.length + 1, "@$ann")
                )
            }
        }
    }

    private fun provideScopeCompletions(prefix: String, sourceCode: String, publisher: CompletionPublisher) {
        // 1. Kotlin Keywords & Snippets
        for (kw in KotlinStdlibCatalog.kotlinKeywords) {
            if (prefix.isEmpty() || kw.name.startsWith(prefix, ignoreCase = true)) {
                publisher.addItem(
                    SimpleCompletionItem(
                        kw.name,
                        "${kw.signature} (Keyword)",
                        prefix.length,
                        kw.snippet
                    )
                )
            }
        }

        // 2. Global stdlib functions (println, listOf, etc.)
        for (gf in KotlinStdlibCatalog.globalFunctions) {
            if (prefix.isEmpty() || gf.name.startsWith(prefix, ignoreCase = true)) {
                publisher.addItem(
                    SimpleCompletionItem(
                        gf.name,
                        "${gf.signature} (Stdlib Function)",
                        prefix.length,
                        gf.snippet
                    )
                )
            }
        }

        // 3. Types (Int, String, List, View, Context, etc.)
        for ((name, type) in KotlinStdlibCatalog.types) {
            if (prefix.isEmpty() || name.startsWith(prefix, ignoreCase = true)) {
                publisher.addItem(
                    SimpleCompletionItem(
                        type.simpleName,
                        "${type.fqName} (Type)",
                        prefix.length,
                        type.simpleName
                    )
                )
            }
        }

        // 4. In-file parsed symbols (Local variables, parameters, functions, classes)
        try {
            val psi = parser.parse(sourceCode)
            for (decl in psi.declarations) {
                if (prefix.isEmpty() || decl.name.startsWith(prefix, ignoreCase = true)) {
                    val kind = decl::class.simpleName ?: "Declaration"
                    publisher.addItem(
                        SimpleCompletionItem(
                            decl.name,
                            "${decl.name} ($kind in current file)",
                            prefix.length,
                            decl.name
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // Ignore parse errors during completion
        }

        // 5. Workspace symbols (across other Kotlin files in the project)
        if (workspaceIndex != null) {
            for (c in workspaceIndex.getAllWorkspaceClasses()) {
                if (prefix.isEmpty() || c.name.startsWith(prefix, ignoreCase = true)) {
                    publisher.addItem(
                        SimpleCompletionItem(
                            c.name,
                            "${c.name} (${c.kind} - Workspace)",
                            prefix.length,
                            c.name
                        )
                    )
                }
            }
            for (f in workspaceIndex.getAllWorkspaceFunctions()) {
                if (prefix.isEmpty() || f.name.startsWith(prefix, ignoreCase = true)) {
                    publisher.addItem(
                        SimpleCompletionItem(
                            f.name,
                            "${f.name}() (Function - Workspace)",
                            prefix.length,
                            "${f.name}()"
                        )
                    )
                }
            }
        }
    }
}
