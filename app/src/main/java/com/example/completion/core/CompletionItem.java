package com.example.completion.core;

import java.util.Objects;

/**
 * Model representing an individual completion candidate suggested to the user.
 */
public final class CompletionItem implements Comparable<CompletionItem> {

    private final String label;
    private final String insertText;
    private final String detail;
    private final String documentation;
    private final SymbolKind kind;
    private final SymbolOrigin origin;
    private final int score;
    private final int priority;
    private final String importToInsert;
    private final String type;

    private CompletionItem(Builder builder) {
        this.label = Objects.requireNonNull(builder.label, "label cannot be null");
        this.insertText = builder.insertText != null ? builder.insertText : builder.label;
        this.detail = builder.detail != null ? builder.detail : "";
        this.documentation = builder.documentation != null ? builder.documentation : "";
        this.kind = builder.kind != null ? builder.kind : SymbolKind.UNKNOWN;
        this.origin = builder.origin != null ? builder.origin : SymbolOrigin.SOURCE;
        this.score = builder.score;
        this.priority = builder.priority;
        this.importToInsert = builder.importToInsert;
        this.type = builder.type != null ? builder.type : "";
    }

    public String getLabel() {
        return label;
    }

    public String getInsertText() {
        return insertText;
    }

    public String getDetail() {
        return detail;
    }

    public String getDocumentation() {
        return documentation;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public SymbolOrigin getOrigin() {
        return origin;
    }

    public int getScore() {
        return score;
    }

    public int getPriority() {
        return priority;
    }

    public String getImportToInsert() {
        return importToInsert;
    }

    public String getType() {
        return type;
    }

    public CompletionItem withScore(int newScore) {
        return builder(this).score(newScore).build();
    }

    @Override
    public int compareTo(CompletionItem other) {
        // Highest score first
        if (this.score != other.score) {
            return Integer.compare(other.score, this.score);
        }
        // Highest priority first
        if (this.priority != other.priority) {
            return Integer.compare(other.priority, this.priority);
        }
        // Alphabetical by label
        return this.label.compareToIgnoreCase(other.label);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompletionItem that = (CompletionItem) o;
        return Objects.equals(label, that.label) &&
                Objects.equals(insertText, that.insertText) &&
                Objects.equals(detail, that.detail) &&
                kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, insertText, detail, kind);
    }

    @Override
    public String toString() {
        return "CompletionItem{" +
                "label='" + label + '\'' +
                ", kind=" + kind +
                ", origin=" + origin +
                ", score=" + score +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String label, SymbolKind kind) {
        return new Builder().label(label).kind(kind);
    }

    public static Builder builder(CompletionItem copy) {
        return new Builder()
                .label(copy.label)
                .insertText(copy.insertText)
                .detail(copy.detail)
                .documentation(copy.documentation)
                .kind(copy.kind)
                .origin(copy.origin)
                .score(copy.score)
                .priority(copy.priority)
                .importToInsert(copy.importToInsert)
                .type(copy.type);
    }

    public static final class Builder {
        private String label;
        private String insertText;
        private String detail;
        private String documentation;
        private SymbolKind kind = SymbolKind.UNKNOWN;
        private SymbolOrigin origin = SymbolOrigin.SOURCE;
        private int score = 0;
        private int priority = 0;
        private String importToInsert;
        private String type;

        public Builder label(String label) {
            this.label = label;
            return this;
        }

        public Builder insertText(String insertText) {
            this.insertText = insertText;
            return this;
        }

        public Builder detail(String detail) {
            this.detail = detail;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder kind(SymbolKind kind) {
            this.kind = kind;
            return this;
        }

        public Builder origin(SymbolOrigin origin) {
            this.origin = origin;
            return this;
        }

        public Builder score(int score) {
            this.score = score;
            return this;
        }

        public Builder priority(int priority) {
            this.priority = priority;
            return this;
        }

        public Builder importToInsert(String importToInsert) {
            this.importToInsert = importToInsert;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public CompletionItem build() {
            return new CompletionItem(this);
        }
    }
}
