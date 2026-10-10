package com.example.completion.psi

import com.example.completion.core.CompletionRequest

import java.util.regex.Pattern

/**
 * Resolves the precise syntactic context and receiver from the cursor offset and source AST.
 */
class PsiContextResolver {

    fun resolveContext(request: CompletionRequest?, parsedFile: ParsedKotlinFile?): PsiCompletionContext {
        if (request == null) {
            return PsiCompletionContext.builder().contextType(CompletionContextType.UNKNOWN).build()
        }

        val content = request.content ?: ""
        val offset = request.cursorPosition
        val safeOffset = Math.max(0, Math.min(offset, content.length))

        // Extract current line before cursor
        var lineStart = content.lastIndexOf('\n', safeOffset - 1)
        lineStart = if (lineStart == -1) 0 else lineStart + 1
        val linePrefix = content.substring(lineStart, safeOffset)

        // 1. Check String literal or Comment
        if (isInString(content, safeOffset)) {
            return PsiCompletionContext.builder()
                .contextType(CompletionContextType.STRING)
                .offset(safeOffset)
                .prefix(request.prefix)
                .parsedFile(parsedFile)
                .build()
        }

        if (isInComment(content, safeOffset, linePrefix)) {
            return PsiCompletionContext.builder()
                .contextType(CompletionContextType.COMMENT)
                .offset(safeOffset)
                .prefix(request.prefix)
                .parsedFile(parsedFile)
                .build()
        }

        // 2. Check Import statement
        val importMatcher = IMPORT_LINE_PATTERN.matcher(linePrefix)
        if (importMatcher.matches()) {
            val impPrefix = importMatcher.group(1)
            return PsiCompletionContext.builder()
                .contextType(CompletionContextType.IMPORT)
                .prefix(impPrefix)
                .offset(safeOffset)
                .line(request.line)
                .column(request.column)
                .parsedFile(parsedFile)
                .build()
        }

        // 3. Check Package statement
        val packageMatcher = PACKAGE_LINE_PATTERN.matcher(linePrefix)
        if (packageMatcher.matches()) {
            val pkgPrefix = packageMatcher.group(1)
            return PsiCompletionContext.builder()
                .contextType(CompletionContextType.PACKAGE)
                .prefix(pkgPrefix)
                .offset(safeOffset)
                .line(request.line)
                .column(request.column)
                .parsedFile(parsedFile)
                .build()
        }

        // 4. Check Dot Qualified Member Access (e.g., "user.name", "text.len", "getUser().")
        val dotMatcher = DOT_ACCESS_PATTERN.matcher(linePrefix)
        if (dotMatcher.find()) {
            val receiver = dotMatcher.group(1)
            val memberPrefix = dotMatcher.group(2)
            return PsiCompletionContext.builder()
                .contextType(CompletionContextType.MEMBER_ACCESS)
                .receiver(receiver)
                .prefix(memberPrefix)
                .offset(safeOffset)
                .line(request.line)
                .column(request.column)
                .parsedFile(parsedFile)
                .build()
        }

        // 5. Default Expression / Reference
        val targetElem = parsedFile?.findElementAt(safeOffset)
        return PsiCompletionContext.builder()
            .contextType(CompletionContextType.REFERENCE)
            .prefix(request.prefix)
            .targetElement(targetElem)
            .offset(safeOffset)
            .line(request.line)
            .column(request.column)
            .parsedFile(parsedFile)
            .build()
    }

    private fun isInString(content: String, offset: Int): Boolean {
        var quoteCount = 0
        var lineStart = content.lastIndexOf('\n', offset - 1)
        lineStart = if (lineStart == -1) 0 else lineStart + 1
        for (i in lineStart until offset) {
            if (content[i] == '"' && (i == 0 || content[i - 1] != '\\')) {
                quoteCount++
            }
        }
        return (quoteCount % 2) != 0
    }

    private fun isInComment(content: String, offset: Int, linePrefix: String): Boolean {
        if (linePrefix.trim().startsWith("//")) {
            return true
        }
        val blockCommentStart = content.lastIndexOf("/*", offset)
        val blockCommentEnd = content.lastIndexOf("*/", offset)
        return blockCommentStart > blockCommentEnd
    }

    companion object {
        private val IMPORT_LINE_PATTERN = Pattern.compile("^[ \\t]*import\\s+([a-zA-Z0-9_.]*)$")
        private val PACKAGE_LINE_PATTERN = Pattern.compile("^[ \\t]*package\\s+([a-zA-Z0-9_.]*)$")
        private val DOT_ACCESS_PATTERN = Pattern.compile("([a-zA-Z0-9_]+(?:\\([^)]*\\))?)\\.([a-zA-Z0-9_]*)$")
    }
}
