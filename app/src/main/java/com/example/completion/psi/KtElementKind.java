package com.example.completion.psi;

/**
 * Kind of element in the Kotlin PSI / AST hierarchy.
 */
public enum KtElementKind {
    FILE,
    PACKAGE_DIRECTIVE,
    IMPORT_DIRECTIVE,
    CLASS,
    INTERFACE,
    OBJECT,
    ENUM_CLASS,
    ENUM_ENTRY,
    TYPE_ALIAS,
    FUNCTION,
    PROPERTY,
    PARAMETER,
    BLOCK,
    CALL_EXPRESSION,
    DOT_QUALIFIED_EXPRESSION,
    NAME_REFERENCE,
    VARIABLE_DECLARATION,
    STRING_LITERAL,
    COMMENT
}
