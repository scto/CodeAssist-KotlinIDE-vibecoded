package com.example.completion.core;

/**
 * Enumeration representing the origin source of a symbol or completion item.
 */
public enum SymbolOrigin {
    SOURCE,
    JAR,
    KOTLIN_METADATA,
    BUILTIN,
    LOCAL,
    KEYWORD
}
