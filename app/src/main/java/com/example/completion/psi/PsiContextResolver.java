package com.example.completion.psi;

import com.example.completion.core.CompletionRequest;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves the precise syntactic context and receiver from the cursor offset and source AST.
 */
public class PsiContextResolver {

    private static final Pattern IMPORT_LINE_PATTERN = Pattern.compile("^[ \\t]*import\\s+([a-zA-Z0-9_.]*)$");
    private static final Pattern PACKAGE_LINE_PATTERN = Pattern.compile("^[ \\t]*package\\s+([a-zA-Z0-9_.]*)$");
    private static final Pattern DOT_ACCESS_PATTERN = Pattern.compile("([a-zA-Z0-9_]+(?:\\([^)]*\\))?)\\.([a-zA-Z0-9_]*)$");

    public PsiCompletionContext resolveContext(CompletionRequest request, ParsedKotlinFile parsedFile) {
        if (request == null) {
            return PsiCompletionContext.builder().contextType(CompletionContextType.UNKNOWN).build();
        }

        String content = request.getContent();
        int offset = request.getCursorPosition();
        int safeOffset = Math.max(0, Math.min(offset, content.length()));

        // Extract current line before cursor
        int lineStart = content.lastIndexOf('\n', safeOffset - 1);
        lineStart = lineStart == -1 ? 0 : lineStart + 1;
        String linePrefix = content.substring(lineStart, safeOffset);

        // 1. Check String literal or Comment
        if (isInString(content, safeOffset)) {
            return PsiCompletionContext.builder()
                    .contextType(CompletionContextType.STRING)
                    .offset(safeOffset)
                    .prefix(request.getPrefix())
                    .parsedFile(parsedFile)
                    .build();
        }

        if (isInComment(content, safeOffset, linePrefix)) {
            return PsiCompletionContext.builder()
                    .contextType(CompletionContextType.COMMENT)
                    .offset(safeOffset)
                    .prefix(request.getPrefix())
                    .parsedFile(parsedFile)
                    .build();
        }

        // 2. Check Import statement
        Matcher importMatcher = IMPORT_LINE_PATTERN.matcher(linePrefix);
        if (importMatcher.matches()) {
            String impPrefix = importMatcher.group(1);
            return PsiCompletionContext.builder()
                    .contextType(CompletionContextType.IMPORT)
                    .prefix(impPrefix)
                    .offset(safeOffset)
                    .line(request.getLine())
                    .column(request.getColumn())
                    .parsedFile(parsedFile)
                    .build();
        }

        // 3. Check Package statement
        Matcher packageMatcher = PACKAGE_LINE_PATTERN.matcher(linePrefix);
        if (packageMatcher.matches()) {
            String pkgPrefix = packageMatcher.group(1);
            return PsiCompletionContext.builder()
                    .contextType(CompletionContextType.PACKAGE)
                    .prefix(pkgPrefix)
                    .offset(safeOffset)
                    .line(request.getLine())
                    .column(request.getColumn())
                    .parsedFile(parsedFile)
                    .build();
        }

        // 4. Check Dot Qualified Member Access (e.g., "user.name", "text.len", "getUser().")
        Matcher dotMatcher = DOT_ACCESS_PATTERN.matcher(linePrefix);
        if (dotMatcher.find()) {
            String receiver = dotMatcher.group(1);
            String memberPrefix = dotMatcher.group(2);
            return PsiCompletionContext.builder()
                    .contextType(CompletionContextType.MEMBER_ACCESS)
                    .receiver(receiver)
                    .prefix(memberPrefix)
                    .offset(safeOffset)
                    .line(request.getLine())
                    .column(request.getColumn())
                    .parsedFile(parsedFile)
                    .build();
        }

        // 5. Default Expression / Reference
        KotlinPsiElement targetElem = parsedFile != null ? parsedFile.findElementAt(safeOffset) : null;
        return PsiCompletionContext.builder()
                .contextType(CompletionContextType.REFERENCE)
                .prefix(request.getPrefix())
                .targetElement(targetElem)
                .offset(safeOffset)
                .line(request.getLine())
                .column(request.getColumn())
                .parsedFile(parsedFile)
                .build();
    }

    private boolean isInString(String content, int offset) {
        int quoteCount = 0;
        int lineStart = content.lastIndexOf('\n', offset - 1);
        lineStart = lineStart == -1 ? 0 : lineStart + 1;
        for (int i = lineStart; i < offset; i++) {
            if (content.charAt(i) == '"' && (i == 0 || content.charAt(i - 1) != '\\')) {
                quoteCount++;
            }
        }
        return (quoteCount % 2) != 0;
    }

    private boolean isInComment(String content, int offset, String linePrefix) {
        if (linePrefix.trim().startsWith("//")) {
            return true;
        }
        int blockCommentStart = content.lastIndexOf("/*", offset);
        int blockCommentEnd = content.lastIndexOf("*/", offset);
        return blockCommentStart > blockCommentEnd;
    }
}
