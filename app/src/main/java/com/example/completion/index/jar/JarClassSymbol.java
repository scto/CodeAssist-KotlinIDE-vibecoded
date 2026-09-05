package com.example.completion.index.jar;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;
import com.example.completion.symbol.Symbol;

import java.io.File;
import java.util.Objects;

/**
 * Symbol representing a class or member extracted from a JAR dependency without using ClassLoader.
 */
public class JarClassSymbol extends Symbol {

    private final String jarFilePath;
    private final boolean isInterface;
    private final boolean isEnum;

    public JarClassSymbol(Builder builder) {
        super(builder);
        this.jarFilePath = builder.jarFilePath != null ? builder.jarFilePath : "";
        this.isInterface = builder.isInterface;
        this.isEnum = builder.isEnum;
    }

    public String getJarFilePath() {
        return jarFilePath;
    }

    public boolean isInterface() {
        return isInterface;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends Symbol.Builder {
        private String jarFilePath = "";
        private boolean isInterface = false;
        private boolean isEnum = false;

        public Builder jarFilePath(String jarFilePath) {
            this.jarFilePath = jarFilePath;
            return this;
        }

        public Builder isInterface(boolean isInterface) {
            this.isInterface = isInterface;
            return this;
        }

        public Builder isEnum(boolean isEnum) {
            this.isEnum = isEnum;
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
        public JarClassSymbol build() {
            return new JarClassSymbol(this);
        }
    }
}
