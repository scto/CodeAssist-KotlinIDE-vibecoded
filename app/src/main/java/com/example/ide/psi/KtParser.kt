package com.example.ide.psi

import java.io.File

/**
 * Fault-tolerant Kotlin AST Parser that parses token streams into structured Kotlin PSI models.
 */
class KtParser(private val lexer: KtLexer = KtLexer()) {

    fun parse(sourceCode: String, file: File? = null): KtFilePsi {
        val rawTokens = lexer.tokenizeEntireSource(sourceCode)
        // Filter out whitespace and comments for parsing logic, but keep in token list for indexing
        val meaningfulTokens = rawTokens.filter {
            it.type != KtTokenType.WHITESPACE &&
            it.type != KtTokenType.COMMENT_LINE &&
            it.type != KtTokenType.COMMENT_BLOCK &&
            it.type != KtTokenType.COMMENT_DOC
        }

        val filePsi = KtFilePsi(file = file, allTokens = rawTokens)
        val parser = TokenStreamParser(meaningfulTokens, filePsi)
        parser.parseFile()
        checkBracketBalance(rawTokens, filePsi)
        return filePsi
    }

    private fun checkBracketBalance(tokens: List<KtToken>, filePsi: KtFilePsi) {
        val parenStack = mutableListOf<KtToken>()
        val braceStack = mutableListOf<KtToken>()
        val bracketStack = mutableListOf<KtToken>()

        for (token in tokens) {
            when (token.type) {
                KtTokenType.LPAREN -> parenStack.add(token)
                KtTokenType.RPAREN -> {
                    if (parenStack.isEmpty()) {
                        filePsi.syntaxErrors.add(
                            KtSyntaxError("Unmatched closing parenthesis ')'", token.line + 1, token.column + 1, 1, "Remove unexpected ')'")
                        )
                    } else {
                        parenStack.removeAt(parenStack.lastIndex)
                    }
                }
                KtTokenType.LBRACE -> braceStack.add(token)
                KtTokenType.RBRACE -> {
                    if (braceStack.isEmpty()) {
                        filePsi.syntaxErrors.add(
                            KtSyntaxError("Unmatched closing brace '}'", token.line + 1, token.column + 1, 1, "Remove unexpected '}'")
                        )
                    } else {
                        braceStack.removeAt(braceStack.lastIndex)
                    }
                }
                KtTokenType.LBRACKET -> bracketStack.add(token)
                KtTokenType.RBRACKET -> {
                    if (bracketStack.isEmpty()) {
                        filePsi.syntaxErrors.add(
                            KtSyntaxError("Unmatched closing bracket ']'", token.line + 1, token.column + 1, 1, "Remove unexpected ']'")
                        )
                    } else {
                        bracketStack.removeAt(bracketStack.lastIndex)
                    }
                }
                else -> {}
            }
        }

        for (unclosed in parenStack) {
            filePsi.syntaxErrors.add(
                KtSyntaxError("Unclosed parenthesis '('", unclosed.line + 1, unclosed.column + 1, 1, "Add closing ')'")
            )
        }
        for (unclosed in braceStack) {
            filePsi.syntaxErrors.add(
                KtSyntaxError("Unclosed brace '{'", unclosed.line + 1, unclosed.column + 1, 1, "Add closing '}'")
            )
        }
        for (unclosed in bracketStack) {
            filePsi.syntaxErrors.add(
                KtSyntaxError("Unclosed bracket '['", unclosed.line + 1, unclosed.column + 1, 1, "Add closing ']'")
            )
        }
    }

    private class TokenStreamParser(
        private val tokens: List<KtToken>,
        private val filePsi: KtFilePsi
    ) {
        private var pos = 0

        private fun current(): KtToken? = if (pos < tokens.size) tokens[pos] else null
        private fun peek(offset: Int = 1): KtToken? = if (pos + offset < tokens.size) tokens[pos + offset] else null

        private fun advance(): KtToken? {
            val t = current()
            if (pos < tokens.size) pos++
            return t
        }

        private fun match(type: KtTokenType): Boolean {
            if (current()?.type == type) {
                advance()
                return true
            }
            return false
        }

        fun parseFile() {
            // 1. Package header
            if (current()?.type == KtTokenType.KEYWORD_PACKAGE) {
                val pkgToken = advance()!!
                val sb = StringBuilder()
                var lastToken: KtToken? = null
                while (current() != null && (current()!!.type == KtTokenType.IDENTIFIER || current()!!.type == KtTokenType.DOT)) {
                    val t = advance()!!
                    sb.append(t.text)
                    lastToken = t
                }
                val fqName = sb.toString()
                filePsi.packageDirective = KtPackageDirective(fqName, pkgToken.line + 1, pkgToken.column + 1)
                match(KtTokenType.SEMICOLON)
            }

            // 2. Import statements
            while (current()?.type == KtTokenType.KEYWORD_IMPORT) {
                val impToken = advance()!!
                val sb = StringBuilder()
                var isAllUnder = false
                var alias: String? = null
                while (current() != null && (current()!!.type == KtTokenType.IDENTIFIER || current()!!.type == KtTokenType.DOT || current()!!.type == KtTokenType.STAR)) {
                    val t = advance()!!
                    if (t.type == KtTokenType.STAR) {
                        isAllUnder = true
                    }
                    sb.append(t.text)
                }
                if (current()?.type == KtTokenType.KEYWORD_AS) {
                    advance()
                    if (current()?.type == KtTokenType.IDENTIFIER) {
                        alias = advance()?.text
                    }
                }
                match(KtTokenType.SEMICOLON)
                filePsi.imports.add(
                    KtImportDirective(
                        importedFqName = sb.toString(),
                        isAllUnder = isAllUnder,
                        alias = alias,
                        line = impToken.line + 1,
                        column = impToken.column + 1
                    )
                )
            }

            // 3. Top-level declarations
            while (pos < tokens.size) {
                val decl = parseDeclaration()
                if (decl != null) {
                    filePsi.declarations.add(decl)
                } else {
                    advance() // recover from unexpected tokens
                }
            }
        }

        private fun parseDeclaration(): KtDeclaration? {
            val modifiers = mutableListOf<String>()
            val annotations = mutableListOf<String>()

            // Collect annotations and modifiers
            while (pos < tokens.size) {
                val t = current() ?: break
                if (t.type == KtTokenType.ANNOTATION) {
                    annotations.add(t.text)
                    advance()
                } else if (isModifier(t.type)) {
                    modifiers.add(t.text)
                    advance()
                } else {
                    break
                }
            }

            val head = current() ?: return null

            return when (head.type) {
                KtTokenType.KEYWORD_CLASS,
                KtTokenType.KEYWORD_INTERFACE,
                KtTokenType.KEYWORD_OBJECT,
                KtTokenType.KEYWORD_DATA,
                KtTokenType.KEYWORD_ENUM,
                KtTokenType.KEYWORD_SEALED,
                KtTokenType.KEYWORD_COMPANION -> {
                    parseClassOrObject(modifiers, annotations)
                }
                KtTokenType.KEYWORD_FUN -> {
                    parseFunction(modifiers, annotations)
                }
                KtTokenType.KEYWORD_VAL,
                KtTokenType.KEYWORD_VAR -> {
                    parseProperty(modifiers, annotations)
                }
                else -> null
            }
        }

        private fun isModifier(type: KtTokenType): Boolean {
            return when (type) {
                KtTokenType.KEYWORD_PUBLIC,
                KtTokenType.KEYWORD_PRIVATE,
                KtTokenType.KEYWORD_PROTECTED,
                KtTokenType.KEYWORD_INTERNAL,
                KtTokenType.KEYWORD_OVERRIDE,
                KtTokenType.KEYWORD_ABSTRACT,
                KtTokenType.KEYWORD_FINAL,
                KtTokenType.KEYWORD_OPEN,
                KtTokenType.KEYWORD_INLINE,
                KtTokenType.KEYWORD_SUSPEND,
                KtTokenType.KEYWORD_TAILREC,
                KtTokenType.KEYWORD_OPERATOR,
                KtTokenType.KEYWORD_INFIX,
                KtTokenType.KEYWORD_CONST,
                KtTokenType.KEYWORD_LATEINIT,
                KtTokenType.KEYWORD_DATA,
                KtTokenType.KEYWORD_ENUM,
                KtTokenType.KEYWORD_SEALED -> true
                else -> false
            }
        }

        private fun parseClassOrObject(modifiers: List<String>, annotations: List<String>): KtClassOrObject? {
            var kind = KtClassOrObject.ClassKind.CLASS
            if (match(KtTokenType.KEYWORD_DATA)) {
                kind = KtClassOrObject.ClassKind.DATA_CLASS
                match(KtTokenType.KEYWORD_CLASS)
            } else if (match(KtTokenType.KEYWORD_ENUM)) {
                kind = KtClassOrObject.ClassKind.ENUM_CLASS
                match(KtTokenType.KEYWORD_CLASS)
            } else if (match(KtTokenType.KEYWORD_SEALED)) {
                kind = KtClassOrObject.ClassKind.SEALED_CLASS
                match(KtTokenType.KEYWORD_CLASS)
            } else if (match(KtTokenType.KEYWORD_INTERFACE)) {
                kind = KtClassOrObject.ClassKind.INTERFACE
            } else if (match(KtTokenType.KEYWORD_COMPANION)) {
                kind = KtClassOrObject.ClassKind.COMPANION_OBJECT
                match(KtTokenType.KEYWORD_OBJECT)
            } else if (match(KtTokenType.KEYWORD_OBJECT)) {
                kind = KtClassOrObject.ClassKind.OBJECT
            } else if (match(KtTokenType.KEYWORD_CLASS)) {
                kind = KtClassOrObject.ClassKind.CLASS
            }

            val nameToken = if (current()?.type == KtTokenType.IDENTIFIER) advance() else null
            val name = nameToken?.text ?: if (kind == KtClassOrObject.ClassKind.COMPANION_OBJECT) "Companion" else "Anonymous"
            val line = nameToken?.line?.plus(1) ?: (current()?.line?.plus(1) ?: 1)
            val column = nameToken?.column?.plus(1) ?: 1

            val primaryParams = mutableListOf<KtParameter>()
            // Check primary constructor parameters: class Foo(val a: Int, var b: String)
            if (current()?.type == KtTokenType.LPAREN) {
                advance()
                while (pos < tokens.size && current()?.type != KtTokenType.RPAREN) {
                    val param = parseParameter()
                    if (param != null) {
                        primaryParams.add(param)
                    }
                    if (current()?.type == KtTokenType.COMMA) {
                        advance()
                    } else if (current()?.type != KtTokenType.RPAREN) {
                        advance()
                    }
                }
                match(KtTokenType.RPAREN)
            }

            // Check super types (: BaseClass(), InterfaceA, InterfaceB)
            val superTypes = mutableListOf<String>()
            if (current()?.type == KtTokenType.COLON) {
                advance()
                while (pos < tokens.size && current()?.type != KtTokenType.LBRACE && current()?.type != KtTokenType.SEMICOLON) {
                    if (current()?.type == KtTokenType.IDENTIFIER || current()?.type == KtTokenType.TYPE_ANY || current()?.type == KtTokenType.TYPE_STRING) {
                        val st = advance()?.text ?: ""
                        superTypes.add(st)
                        // Skip generics <...> or constructor calls (...)
                        skipGenericsAndParens()
                    }
                    if (current()?.type == KtTokenType.COMMA) {
                        advance()
                    } else if (current()?.type != KtTokenType.LBRACE) {
                        advance()
                    }
                }
            }

            val functions = mutableListOf<KtFunction>()
            val properties = mutableListOf<KtProperty>()
            val innerClasses = mutableListOf<KtClassOrObject>()

            // Parse Class Body
            if (current()?.type == KtTokenType.LBRACE) {
                advance()
                var braceDepth = 1
                while (pos < tokens.size && braceDepth > 0) {
                    val t = current() ?: break
                    if (t.type == KtTokenType.RBRACE) {
                        braceDepth--
                        advance()
                        if (braceDepth == 0) break
                    } else if (t.type == KtTokenType.LBRACE) {
                        braceDepth++
                        advance()
                    } else {
                        val member = parseDeclaration()
                        when (member) {
                            is KtFunction -> functions.add(member)
                            is KtProperty -> properties.add(member)
                            is KtClassOrObject -> innerClasses.add(member)
                            else -> advance()
                        }
                    }
                }
            }

            return KtClassOrObject(
                name = name,
                kind = kind,
                modifiers = modifiers,
                annotations = annotations,
                superTypes = superTypes,
                primaryConstructorParams = primaryParams,
                functions = functions,
                properties = properties,
                innerClasses = innerClasses,
                line = line,
                column = column
            )
        }

        private fun parseFunction(modifiers: List<String>, annotations: List<String>): KtFunction? {
            val funToken = if (match(KtTokenType.KEYWORD_FUN)) tokens[pos - 1] else return null
            val isSuspend = modifiers.contains("suspend")
            val isInline = modifiers.contains("inline")
            val isOverride = modifiers.contains("override")

            // Skip type parameters <T>
            skipGenerics()

            val nameToken = if (current()?.type == KtTokenType.IDENTIFIER) advance() else null
            val name = nameToken?.text ?: "unnamed_function"
            val line = nameToken?.line?.plus(1) ?: (funToken.line + 1)
            val col = nameToken?.column?.plus(1) ?: (funToken.column + 1)

            val params = mutableListOf<KtParameter>()
            if (current()?.type == KtTokenType.LPAREN) {
                advance()
                while (pos < tokens.size && current()?.type != KtTokenType.RPAREN) {
                    val p = parseParameter()
                    if (p != null) params.add(p)
                    if (current()?.type == KtTokenType.COMMA) {
                        advance()
                    } else if (current()?.type != KtTokenType.RPAREN) {
                        advance()
                    }
                }
                match(KtTokenType.RPAREN)
            }

            var returnType = "Unit"
            if (current()?.type == KtTokenType.COLON) {
                advance()
                if (current() != null && isTypeToken(current()!!.type)) {
                    returnType = advance()?.text ?: "Unit"
                    if (current()?.type == KtTokenType.QUESTION) {
                        returnType += "?"
                        advance()
                    }
                }
            }

            val localVars = mutableListOf<KtProperty>()
            val bodyStatements = mutableListOf<String>()

            // Parse body: { ... } or = expression
            if (current()?.type == KtTokenType.LBRACE) {
                advance()
                var braceDepth = 1
                while (pos < tokens.size && braceDepth > 0) {
                    val t = current() ?: break
                    if (t.type == KtTokenType.LBRACE) {
                        braceDepth++
                        advance()
                    } else if (t.type == KtTokenType.RBRACE) {
                        braceDepth--
                        advance()
                    } else if (t.type == KtTokenType.KEYWORD_VAL || t.type == KtTokenType.KEYWORD_VAR) {
                        val prop = parseProperty(emptyList(), emptyList())
                        if (prop != null) localVars.add(prop)
                    } else {
                        advance()
                    }
                }
            } else if (current()?.type == KtTokenType.ASSIGN) {
                advance()
                while (pos < tokens.size && current()?.type != KtTokenType.SEMICOLON && current()?.type != KtTokenType.KEYWORD_FUN && current()?.type != KtTokenType.KEYWORD_VAL && current()?.type != KtTokenType.KEYWORD_VAR && current()?.type != KtTokenType.RBRACE) {
                    advance()
                }
            }

            return KtFunction(
                name = name,
                parameters = params,
                returnType = returnType,
                isSuspend = isSuspend,
                isInline = isInline,
                isOverride = isOverride,
                modifiers = modifiers,
                annotations = annotations,
                localVariables = localVars,
                bodyStatements = bodyStatements,
                line = line,
                column = col
            )
        }

        private fun parseProperty(modifiers: List<String>, annotations: List<String>): KtProperty? {
            val isVar = current()?.type == KtTokenType.KEYWORD_VAR
            if (!match(KtTokenType.KEYWORD_VAL) && !match(KtTokenType.KEYWORD_VAR)) return null

            val nameToken = if (current()?.type == KtTokenType.IDENTIFIER) advance() else null
            val name = nameToken?.text ?: "unnamed_property"
            val line = nameToken?.line?.plus(1) ?: 1
            val col = nameToken?.column?.plus(1) ?: 1

            var type = "Any"
            if (current()?.type == KtTokenType.COLON) {
                advance()
                if (current() != null && isTypeToken(current()!!.type)) {
                    type = advance()?.text ?: "Any"
                    if (current()?.type == KtTokenType.QUESTION) {
                        type += "?"
                        advance()
                    }
                }
            }

            var initializer: String? = null
            if (current()?.type == KtTokenType.ASSIGN) {
                advance()
                val sb = StringBuilder()
                while (pos < tokens.size && current()?.type != KtTokenType.SEMICOLON && current()?.type != KtTokenType.KEYWORD_VAL && current()?.type != KtTokenType.KEYWORD_VAR && current()?.type != KtTokenType.KEYWORD_FUN && current()?.type != KtTokenType.RBRACE) {
                    val t = advance()!!
                    sb.append(t.text).append(" ")
                    if (t.type == KtTokenType.LBRACE) {
                        // skip block if lambda initializer
                        var d = 1
                        while (pos < tokens.size && d > 0) {
                            val inner = advance()!!
                            if (inner.type == KtTokenType.LBRACE) d++
                            else if (inner.type == KtTokenType.RBRACE) d--
                            sb.append(inner.text).append(" ")
                        }
                    }
                }
                initializer = sb.toString().trim()
            }
            match(KtTokenType.SEMICOLON)

            return KtProperty(
                name = name,
                type = type,
                isVar = isVar,
                isOverride = modifiers.contains("override"),
                initializer = initializer,
                modifiers = modifiers,
                annotations = annotations,
                line = line,
                column = col
            )
        }

        private fun parseParameter(): KtParameter? {
            var isVararg = false
            var isValOrVar = false
            var isVar = false

            if (match(KtTokenType.KEYWORD_VARARG)) isVararg = true
            if (match(KtTokenType.KEYWORD_VAL)) {
                isValOrVar = true
                isVar = false
            } else if (match(KtTokenType.KEYWORD_VAR)) {
                isValOrVar = true
                isVar = true
            }

            val nameToken = if (current()?.type == KtTokenType.IDENTIFIER) advance() else return null
            if (nameToken == null) return null
            val name = nameToken.text
            var type = "Any"

            if (match(KtTokenType.COLON)) {
                if (current() != null && isTypeToken(current()!!.type)) {
                    type = advance()?.text ?: "Any"
                    if (current()?.type == KtTokenType.QUESTION) {
                        type += "?"
                        advance()
                    }
                }
            }

            var defaultValue: String? = null
            if (match(KtTokenType.ASSIGN)) {
                val sb = StringBuilder()
                while (pos < tokens.size && current()?.type != KtTokenType.COMMA && current()?.type != KtTokenType.RPAREN) {
                    sb.append(advance()?.text).append(" ")
                }
                defaultValue = sb.toString().trim()
            }

            return KtParameter(
                name = name,
                type = type,
                defaultValue = defaultValue,
                isVararg = isVararg,
                isValOrVar = isValOrVar,
                isVar = isVar,
                line = nameToken.line + 1,
                column = nameToken.column + 1
            )
        }

        private fun isTypeToken(type: KtTokenType): Boolean {
            return type == KtTokenType.IDENTIFIER ||
                   type == KtTokenType.TYPE_INT ||
                   type == KtTokenType.TYPE_STRING ||
                   type == KtTokenType.TYPE_BOOLEAN ||
                   type == KtTokenType.TYPE_DOUBLE ||
                   type == KtTokenType.TYPE_FLOAT ||
                   type == KtTokenType.TYPE_LONG ||
                   type == KtTokenType.TYPE_CHAR ||
                   type == KtTokenType.TYPE_BYTE ||
                   type == KtTokenType.TYPE_SHORT ||
                   type == KtTokenType.TYPE_LIST ||
                   type == KtTokenType.TYPE_MAP ||
                   type == KtTokenType.TYPE_SET ||
                   type == KtTokenType.TYPE_ARRAY ||
                   type == KtTokenType.TYPE_ANY ||
                   type == KtTokenType.TYPE_UNIT ||
                   type == KtTokenType.TYPE_NOTHING
        }

        private fun skipGenerics() {
            if (current()?.type == KtTokenType.LT) {
                var depth = 1
                advance()
                while (pos < tokens.size && depth > 0) {
                    val t = advance() ?: break
                    if (t.type == KtTokenType.LT) depth++
                    else if (t.type == KtTokenType.GT) depth--
                }
            }
        }

        private fun skipGenericsAndParens() {
            skipGenerics()
            if (current()?.type == KtTokenType.LPAREN) {
                var depth = 1
                advance()
                while (pos < tokens.size && depth > 0) {
                    val t = advance() ?: break
                    if (t.type == KtTokenType.LPAREN) depth++
                    else if (t.type == KtTokenType.RPAREN) depth--
                }
            }
        }
    }
}
