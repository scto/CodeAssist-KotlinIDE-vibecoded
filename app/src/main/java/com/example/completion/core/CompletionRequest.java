package com.example.completion.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable request model containing all context information for a code completion query.
 */
public final class CompletionRequest {

    private final String content;
    private final int cursorPosition;
    private final String filePath;
    private final String prefix;
    private final int line;
    private final int column;
    private final Map<String, Object> extraAttributes;

    private CompletionRequest(Builder builder) {
        this.content = Objects.requireNonNull(builder.content, "content cannot be null");
        this.cursorPosition = Math.max(0, Math.min(builder.cursorPosition, builder.content.length()));
        this.filePath = builder.filePath != null ? builder.filePath : "Untitled.kt";
        this.prefix = builder.prefix != null ? builder.prefix : extractPrefix(this.content, this.cursorPosition);
        this.line = builder.line >= 0 ? builder.line : calculateLine(this.content, this.cursorPosition);
        this.column = builder.column >= 0 ? builder.column : calculateColumn(this.content, this.cursorPosition);
        this.extraAttributes = Collections.unmodifiableMap(new HashMap<>(builder.extraAttributes));
    }

    public String getContent() {
        return content;
    }

    public int getCursorPosition() {
        return cursorPosition;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getPrefix() {
        return prefix;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public Object getAttribute(String key) {
        return extraAttributes.get(key);
    }

    public Map<String, Object> getExtraAttributes() {
        return extraAttributes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static CompletionRequest create(String content, int cursorPosition, String filePath) {
        return builder()
                .content(content)
                .cursorPosition(cursorPosition)
                .filePath(filePath)
                .build();
    }

    public static CompletionRequest create(String filePath, String content, int cursorPosition) {
        return builder()
                .content(content)
                .cursorPosition(cursorPosition)
                .filePath(filePath)
                .build();
    }

    public static CompletionRequest create(String content, int cursorPosition) {
        return builder()
                .content(content)
                .cursorPosition(cursorPosition)
                .build();
    }

    private static String extractPrefix(String text, int offset) {
        if (text == null || offset <= 0 || offset > text.length()) {
            return "";
        }
        int start = offset;
        while (start > 0) {
            char c = text.charAt(start - 1);
            if (Character.isJavaIdentifierPart(c)) {
                start--;
            } else {
                break;
            }
        }
        return text.substring(start, offset);
    }

    private static int calculateLine(String text, int offset) {
        int line = 0;
        int max = Math.min(offset, text.length());
        for (int i = 0; i < max; i++) {
            if (text.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }

    private static int calculateColumn(String text, int offset) {
        int max = Math.min(offset, text.length());
        int lastNewLine = text.lastIndexOf('\n', max - 1);
        if (lastNewLine == -1) {
            return max;
        }
        return max - (lastNewLine + 1);
    }

    public static final class Builder {
        private String content = "";
        private int cursorPosition = 0;
        private String filePath = "Untitled.kt";
        private String prefix;
        private int line = -1;
        private int column = -1;
        private final Map<String, Object> extraAttributes = new HashMap<>();

        public Builder content(String content) {
            this.content = content != null ? content : "";
            return this;
        }

        public Builder cursorPosition(int cursorPosition) {
            this.cursorPosition = cursorPosition;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder line(int line) {
            this.line = line;
            return this;
        }

        public Builder column(int column) {
            this.column = column;
            return this;
        }

        public Builder attribute(String key, Object value) {
            if (key != null && value != null) {
                this.extraAttributes.put(key, value);
            }
            return this;
        }

        public CompletionRequest build() {
            return new CompletionRequest(this);
        }
    }
}
