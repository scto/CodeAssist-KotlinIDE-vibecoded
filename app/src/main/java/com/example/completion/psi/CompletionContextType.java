package com.example.completion.psi;

/**
 * Enumeration of semantic syntax contexts detected at the editor cursor position.
 */
public enum CompletionContextType {
    UNKNOWN,
    EXPRESSION,
    REFERENCE,
    MEMBER_ACCESS,
    IMPORT,
    PACKAGE,
    TYPE,
    CALL,
    STRING,
    COMMENT
}
