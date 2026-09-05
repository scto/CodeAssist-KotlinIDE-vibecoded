package com.example.completion.resolver;

import java.util.Objects;

/**
 * Result of heuristic type resolution on an expression or variable.
 */
public class TypeResolutionResult {

    private final String typeName;
    private final String qualifiedTypeName;
    private final boolean isNullable;
    private final boolean isResolved;

    public TypeResolutionResult(String typeName, String qualifiedTypeName, boolean isNullable, boolean isResolved) {
        this.typeName = typeName != null ? typeName : "Any";
        this.qualifiedTypeName = qualifiedTypeName != null ? qualifiedTypeName : this.typeName;
        this.isNullable = isNullable;
        this.isResolved = isResolved;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getQualifiedTypeName() {
        return qualifiedTypeName;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public static TypeResolutionResult resolved(String typeName) {
        return new TypeResolutionResult(typeName, typeName, false, true);
    }

    public static TypeResolutionResult unknown() {
        return new TypeResolutionResult("Any", "kotlin.Any", false, false);
    }

    @Override
    public String toString() {
        return "TypeResolutionResult{" +
                "typeName='" + typeName + '\'' +
                ", isResolved=" + isResolved +
                '}';
    }
}
