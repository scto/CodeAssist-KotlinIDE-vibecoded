package com.example.completion.index.source;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.symbol.Symbol;

import java.io.File;
import java.util.List;

/**
 * Symbol extracted from project source files with file path and container metadata.
 */
public class ProjectSymbol extends Symbol {

    private final String sourceFilePath;
    private final long lastModified;

    public ProjectSymbol(Builder builder) {
        super(builder);
        this.sourceFilePath = builder.sourceFilePath != null ? builder.sourceFilePath : "";
        this.lastModified = builder.lastModified;
    }

    public String getSourceFilePath() {
        return sourceFilePath;
    }

    public long getLastModified() {
        return lastModified;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Symbol.Builder {
        private String sourceFilePath = "";
        private long lastModified = 0;

        public Builder sourceFilePath(String sourceFilePath) {
            this.sourceFilePath = sourceFilePath;
            return this;
        }

        public Builder lastModified(long lastModified) {
            this.lastModified = lastModified;
            return this;
        }

        @Override
        public Builder name(String name) {
            super.name(name);
            return this;
        }

        @Override
        public Builder qualifiedName(String qualifiedName) {
            super.qualifiedName(qualifiedName);
            return this;
        }

        @Override
        public Builder kind(SymbolKind kind) {
            super.kind(kind);
            return this;
        }

        @Override
        public Builder origin(SymbolOrigin origin) {
            super.origin(origin);
            return this;
        }

        @Override
        public Builder packageName(String packageName) {
            super.packageName(packageName);
            return this;
        }

        @Override
        public Builder returnType(String returnType) {
            super.returnType(returnType);
            return this;
        }

        @Override
        public Builder parameters(List<String> parameters) {
            super.parameters(parameters);
            return this;
        }

        @Override
        public Builder containerName(String containerName) {
            super.containerName(containerName);
            return this;
        }

        @Override
        public Builder receiverType(String receiverType) {
            super.receiverType(receiverType);
            return this;
        }

        @Override
        public ProjectSymbol build() {
            return new ProjectSymbol(this);
        }
    }
}
