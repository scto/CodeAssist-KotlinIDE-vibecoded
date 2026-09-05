package com.example.completion.psi;

/**
 * Pluggable Parser interface for converting raw Kotlin source code into an AST / PSI model.
 */
public interface KotlinParser {

    /**
     * Parses the Kotlin source code into an AST / PSI representation.
     * Must be error-tolerant and never throw uncaught exceptions on malformed / incomplete user code.
     *
     * @param fileName the name of the file being edited
     * @param source the complete or partial source code
     * @return a structured ParsedKotlinFile instance (never null)
     */
    ParsedKotlinFile parse(String fileName, String source);
}
