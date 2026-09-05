package com.example.completion.symbol;

import com.example.completion.core.SymbolKind;
import com.example.completion.core.SymbolOrigin;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Universal model for any code symbol (class, function, property, parameter, etc.)
 * across Source, JAR, Metadata, and Built-in origins.
 */
public class Symbol {

    private final String name;
    private final String qualifiedName;
    private final SymbolKind kind;
    private final SymbolOrigin origin;
    private final String packageName;
    private final String returnType;
    private final List<String> parameters;
    private final String containerName;
    private final String documentation;
    private final String receiverType; // For extension functions/properties
    private final boolean isStatic;

    public Symbol(Builder builder) {
        this.name = Objects.requireNonNull(builder.name, "name cannot be null");
        this.qualifiedName = builder.qualifiedName != null ? builder.qualifiedName : builder.name;
        this.kind = builder.kind != null ? builder.kind : SymbolKind.UNKNOWN;
        this.origin = builder.origin != null ? builder.origin : SymbolOrigin.SOURCE;
        this.packageName = builder.packageName != null ? builder.packageName : "";
        this.returnType = builder.returnType != null ? builder.returnType : "";
        this.parameters = builder.parameters != null ? Collections.unmodifiableList(builder.parameters) : Collections.emptyList();
        this.containerName = builder.containerName != null ? builder.containerName : "";
        this.documentation = builder.documentation != null ? builder.documentation : "";
        this.receiverType = builder.receiverType != null ? builder.receiverType : "";
        this.isStatic = builder.isStatic;
    }

    public String getName() {
        return name;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public SymbolOrigin getOrigin() {
        return origin;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getDocumentation() {
        return documentation;
    }

    public String getReceiverType() {
        return receiverType;
    }

    public boolean isExtension() {
        return receiverType != null && !receiverType.isEmpty();
    }

    public boolean isStatic() {
        return isStatic;
    }

    public String getParameterSignature() {
        if (parameters == null || parameters.isEmpty()) {
            return "()";
        }
        return "(" + String.join(", ", parameters) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Symbol symbol = (Symbol) o;
        return isStatic == symbol.isStatic &&
                Objects.equals(name, symbol.name) &&
                Objects.equals(qualifiedName, symbol.qualifiedName) &&
                kind == symbol.kind &&
                origin == symbol.origin &&
                Objects.equals(containerName, symbol.containerName) &&
                Objects.equals(parameters, symbol.parameters) &&
                Objects.equals(receiverType, symbol.receiverType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, qualifiedName, kind, origin, containerName, parameters, receiverType, isStatic);
    }

    @Override
    public String toString() {
        return "Symbol{" +
                "name='" + name + '\'' +
                ", qualifiedName='" + qualifiedName + '\'' +
                ", kind=" + kind +
                ", origin=" + origin +
                ", returnType='" + returnType + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String name, SymbolKind kind, SymbolOrigin origin) {
        return new Builder().name(name).kind(kind).origin(origin);
    }

    public static class Builder {
        private String name;
        private String qualifiedName;
        private SymbolKind kind = SymbolKind.UNKNOWN;
        private SymbolOrigin origin = SymbolOrigin.SOURCE;
        private String packageName = "";
        private String returnType = "";
        private List<String> parameters = Collections.emptyList();
        private String containerName = "";
        private String documentation = "";
        private String receiverType = "";
        private boolean isStatic = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder qualifiedName(String qualifiedName) {
            this.qualifiedName = qualifiedName;
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

        public Builder packageName(String packageName) {
            this.packageName = packageName;
            return this;
        }

        public Builder returnType(String returnType) {
            this.returnType = returnType;
            return this;
        }

        public Builder parameters(List<String> parameters) {
            this.parameters = parameters;
            return this;
        }

        public Builder containerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
            return this;
        }

        public Builder receiverType(String receiverType) {
            this.receiverType = receiverType;
            return this;
        }

        public Builder isStatic(boolean isStatic) {
            this.isStatic = isStatic;
            return this;
        }

        public Symbol build() {
            return new Symbol(this);
        }
    }
}
