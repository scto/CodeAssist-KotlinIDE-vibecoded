package com.example.completion.resolver

/**
 * Result of heuristic type resolution on an expression or variable.
 */
class TypeResolutionResult(
    typeName: String?,
    qualifiedTypeName: String?,
    val isNullable: Boolean,
    val isResolved: Boolean
) {
    val typeName: String = typeName ?: "Any"
    val qualifiedTypeName: String = qualifiedTypeName ?: this.typeName

    companion object {
        @JvmStatic
        fun resolved(typeName: String): TypeResolutionResult {
            return TypeResolutionResult(typeName, typeName, isNullable = false, isResolved = true)
        }

        @JvmStatic
        fun unknown(): TypeResolutionResult {
            return TypeResolutionResult("Any", "kotlin.Any", isNullable = false, isResolved = false)
        }
    }

    override fun toString(): String {
        return "TypeResolutionResult{" +
                "typeName='$typeName'" +
                ", isResolved=$isResolved" +
                '}'
    }
}
