package com.example.completion.index.metadata;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.symbol.Symbol;

import java.util.List;

/**
 * Symbol extracted from Kotlin class file metadata (functions, properties, constructors, typealiases).
 */
public class MetadataSymbol extends Symbol {

    private final String kotlinPackage;

    public MetadataSymbol(Builder builder) {
        super(builder);
        this.kotlinPackage = builder.kotlinPackage != null ? builder.kotlinPackage : "";
    }

    public String getKotlinPackage() {
        return kotlinPackage;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Symbol.Builder {
        private String kotlinPackage = "";

        public Builder kotlinPackage(String kotlinPackage) {
            this.kotlinPackage = kotlinPackage;
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
        public MetadataSymbol build() {
            return new MetadataSymbol(this);
        }
    }
}
