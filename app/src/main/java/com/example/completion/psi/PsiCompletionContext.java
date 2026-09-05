package com.example.completion.psi;

import java.util.Objects;

/**
 * Encapsulates the resolved syntactic and semantic context at the cursor position.
 */
public class PsiCompletionContext {

    private final CompletionContextType contextType;
    private final String prefix;
    private final String receiver;
    private final String receiverType;
    private final ParsedKotlinFile parsedFile;
    private final KotlinPsiElement targetElement;
    private final KotlinPsiElement scopeElement;
    private final int offset;
    private final int line;
    private final int column;

    private PsiCompletionContext(Builder builder) {
        this.contextType = Objects.requireNonNull(builder.contextType, "contextType cannot be null");
        this.prefix = builder.prefix != null ? builder.prefix : "";
        this.receiver = builder.receiver != null ? builder.receiver : "";
        this.receiverType = builder.receiverType != null ? builder.receiverType : "";
        this.parsedFile = builder.parsedFile;
        this.targetElement = builder.targetElement;
        this.scopeElement = builder.scopeElement;
        this.offset = builder.offset;
        this.line = builder.line;
        this.column = builder.column;
    }

    public CompletionContextType getContextType() {
        return contextType;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public ParsedKotlinFile getParsedFile() {
        return parsedFile;
    }

    public KotlinPsiElement getTargetElement() {
        return targetElement;
    }

    public KotlinPsiElement getScopeElement() {
        return scopeElement;
    }

    public int getOffset() {
        return offset;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public boolean isMemberAccess() {
        return contextType == CompletionContextType.MEMBER_ACCESS && !receiver.isEmpty();
    }

    public boolean isImport() {
        return contextType == CompletionContextType.IMPORT;
    }

    public boolean isPackage() {
        return contextType == CompletionContextType.PACKAGE;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CompletionContextType contextType = CompletionContextType.UNKNOWN;
        private String prefix = "";
        private String receiver = "";
        private String receiverType = "";
        private ParsedKotlinFile parsedFile;
        private KotlinPsiElement targetElement;
        private KotlinPsiElement scopeElement;
        private int offset;
        private int line;
        private int column;

        public Builder contextType(CompletionContextType contextType) {
            this.contextType = contextType;
            return this;
        }

        public Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public Builder receiver(String receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder receiverType(String receiverType) {
            this.receiverType = receiverType;
            return this;
        }

        public Builder parsedFile(ParsedKotlinFile parsedFile) {
            this.parsedFile = parsedFile;
            return this;
        }

        public Builder targetElement(KotlinPsiElement targetElement) {
            this.targetElement = targetElement;
            return this;
        }

        public Builder scopeElement(KotlinPsiElement scopeElement) {
            this.scopeElement = scopeElement;
            return this;
        }

        public Builder offset(int offset) {
            this.offset = offset;
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

        public PsiCompletionContext build() {
            return new PsiCompletionContext(this);
        }
    }
}
