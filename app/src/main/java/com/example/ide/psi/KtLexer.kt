package com.example.ide.psi

/**
 * Fast Kotlin Lexer / Tokenizer supporting incremental line analysis and full file tokenization.
 */
class KtLexer {

    companion object {
        private val KEYWORDS = mapOf(
            "package" to KtTokenType.KEYWORD_PACKAGE,
            "import" to KtTokenType.KEYWORD_IMPORT,
            "fun" to KtTokenType.KEYWORD_FUN,
            "val" to KtTokenType.KEYWORD_VAL,
            "var" to KtTokenType.KEYWORD_VAR,
            "class" to KtTokenType.KEYWORD_CLASS,
            "interface" to KtTokenType.KEYWORD_INTERFACE,
            "object" to KtTokenType.KEYWORD_OBJECT,
            "companion" to KtTokenType.KEYWORD_COMPANION,
            "data" to KtTokenType.KEYWORD_DATA,
            "enum" to KtTokenType.KEYWORD_ENUM,
            "sealed" to KtTokenType.KEYWORD_SEALED,
            "open" to KtTokenType.KEYWORD_OPEN,
            "override" to KtTokenType.KEYWORD_OVERRIDE,
            "private" to KtTokenType.KEYWORD_PRIVATE,
            "protected" to KtTokenType.KEYWORD_PROTECTED,
            "public" to KtTokenType.KEYWORD_PUBLIC,
            "internal" to KtTokenType.KEYWORD_INTERNAL,
            "abstract" to KtTokenType.KEYWORD_ABSTRACT,
            "final" to KtTokenType.KEYWORD_FINAL,
            "inline" to KtTokenType.KEYWORD_INLINE,
            "tailrec" to KtTokenType.KEYWORD_TAILREC,
            "suspend" to KtTokenType.KEYWORD_SUSPEND,
            "operator" to KtTokenType.KEYWORD_OPERATOR,
            "infix" to KtTokenType.KEYWORD_INFIX,
            "constructor" to KtTokenType.KEYWORD_CONSTRUCTOR,
            "init" to KtTokenType.KEYWORD_INIT,
            "this" to KtTokenType.KEYWORD_THIS,
            "super" to KtTokenType.KEYWORD_SUPER,
            "if" to KtTokenType.KEYWORD_IF,
            "else" to KtTokenType.KEYWORD_ELSE,
            "when" to KtTokenType.KEYWORD_WHEN,
            "try" to KtTokenType.KEYWORD_TRY,
            "catch" to KtTokenType.KEYWORD_CATCH,
            "finally" to KtTokenType.KEYWORD_FINALLY,
            "for" to KtTokenType.KEYWORD_FOR,
            "do" to KtTokenType.KEYWORD_DO,
            "while" to KtTokenType.KEYWORD_WHILE,
            "break" to KtTokenType.KEYWORD_BREAK,
            "continue" to KtTokenType.KEYWORD_CONTINUE,
            "return" to KtTokenType.KEYWORD_RETURN,
            "throw" to KtTokenType.KEYWORD_THROW,
            "is" to KtTokenType.KEYWORD_IS,
            "as" to KtTokenType.KEYWORD_AS,
            "in" to KtTokenType.KEYWORD_IN,
            "by" to KtTokenType.KEYWORD_BY,
            "typealias" to KtTokenType.KEYWORD_TYPEALIAS,
            "where" to KtTokenType.KEYWORD_WHERE,
            "get" to KtTokenType.KEYWORD_GET,
            "set" to KtTokenType.KEYWORD_SET,
            "vararg" to KtTokenType.KEYWORD_VARARG,
            "noinline" to KtTokenType.KEYWORD_NOINLINE,
            "crossinline" to KtTokenType.KEYWORD_CROSSINLINE,
            "reified" to KtTokenType.KEYWORD_REIFIED,
            "const" to KtTokenType.KEYWORD_CONST,
            "lateinit" to KtTokenType.KEYWORD_LATEINIT,
            "dynamic" to KtTokenType.KEYWORD_DYNAMIC,
            "true" to KtTokenType.BOOLEAN_TRUE,
            "false" to KtTokenType.BOOLEAN_FALSE,
            "null" to KtTokenType.NULL_LITERAL
        )

        private val BUILTIN_TYPES = mapOf(
            "Int" to KtTokenType.TYPE_INT,
            "Long" to KtTokenType.TYPE_LONG,
            "Short" to KtTokenType.TYPE_SHORT,
            "Byte" to KtTokenType.TYPE_BYTE,
            "Float" to KtTokenType.TYPE_FLOAT,
            "Double" to KtTokenType.TYPE_DOUBLE,
            "Boolean" to KtTokenType.TYPE_BOOLEAN,
            "Char" to KtTokenType.TYPE_CHAR,
            "String" to KtTokenType.TYPE_STRING,
            "Array" to KtTokenType.TYPE_ARRAY,
            "List" to KtTokenType.TYPE_LIST,
            "Map" to KtTokenType.TYPE_MAP,
            "Set" to KtTokenType.TYPE_SET,
            "Unit" to KtTokenType.TYPE_UNIT,
            "Nothing" to KtTokenType.TYPE_NOTHING,
            "Any" to KtTokenType.TYPE_ANY
        )

        fun isKotlinKeyword(word: String): Boolean = KEYWORDS.containsKey(word)
        fun isBuiltinType(word: String): Boolean = BUILTIN_TYPES.containsKey(word)
        fun getAllKeywords(): Set<String> = KEYWORDS.keys
        fun getAllBuiltinTypes(): Set<String> = BUILTIN_TYPES.keys
    }

    data class LexerState(
        val inBlockComment: Boolean = false,
        val inRawString: Boolean = false
    )

    fun tokenizeLine(lineText: CharSequence, startState: LexerState = LexerState(), lineIndex: Int = 0): Pair<List<KtToken>, LexerState> {
        val tokens = mutableListOf<KtToken>()
        var i = 0
        val len = lineText.length
        var inBlockComment = startState.inBlockComment
        var inRawString = startState.inRawString

        while (i < len) {
            val startCol = i

            // 1. Ongoing Block Comment
            if (inBlockComment) {
                val endIdx = lineText.indexOf("*/", i)
                if (endIdx != -1) {
                    tokens.add(KtToken(KtTokenType.COMMENT_BLOCK, lineText.substring(i, endIdx + 2), lineIndex, startCol, i, endIdx + 2))
                    i = endIdx + 2
                    inBlockComment = false
                } else {
                    tokens.add(KtToken(KtTokenType.COMMENT_BLOCK, lineText.substring(i), lineIndex, startCol, i, len))
                    i = len
                }
                continue
            }

            // 2. Ongoing Raw String
            if (inRawString) {
                val endIdx = lineText.indexOf("\"\"\"", i)
                if (endIdx != -1) {
                    tokens.add(KtToken(KtTokenType.STRING_RAW, lineText.substring(i, endIdx + 3), lineIndex, startCol, i, endIdx + 3))
                    i = endIdx + 3
                    inRawString = false
                } else {
                    tokens.add(KtToken(KtTokenType.STRING_RAW, lineText.substring(i), lineIndex, startCol, i, len))
                    i = len
                }
                continue
            }

            val c = lineText[i]

            // 3. Whitespace
            if (c.isWhitespace()) {
                val start = i
                while (i < len && lineText[i].isWhitespace()) {
                    i++
                }
                tokens.add(KtToken(KtTokenType.WHITESPACE, lineText.substring(start, i), lineIndex, startCol, start, i))
                continue
            }

            // 4. Line comment or Block comment start
            if (c == '/' && i + 1 < len) {
                if (lineText[i + 1] == '/') {
                    tokens.add(KtToken(KtTokenType.COMMENT_LINE, lineText.substring(i), lineIndex, startCol, i, len))
                    i = len
                    continue
                } else if (lineText[i + 1] == '*') {
                    val isDoc = (i + 2 < len && lineText[i + 2] == '*')
                    val endIdx = lineText.indexOf("*/", i + 2)
                    val tokenType = if (isDoc) KtTokenType.COMMENT_DOC else KtTokenType.COMMENT_BLOCK
                    if (endIdx != -1) {
                        tokens.add(KtToken(tokenType, lineText.substring(i, endIdx + 2), lineIndex, startCol, i, endIdx + 2))
                        i = endIdx + 2
                    } else {
                        tokens.add(KtToken(tokenType, lineText.substring(i), lineIndex, startCol, i, len))
                        i = len
                        inBlockComment = true
                    }
                    continue
                }
            }

            // 5. Raw string start """ or Standard String "
            if (c == '"') {
                if (i + 2 < len && lineText[i + 1] == '"' && lineText[i + 2] == '"') {
                    val endIdx = lineText.indexOf("\"\"\"", i + 3)
                    if (endIdx != -1) {
                        tokens.add(KtToken(KtTokenType.STRING_RAW, lineText.substring(i, endIdx + 3), lineIndex, startCol, i, endIdx + 3))
                        i = endIdx + 3
                    } else {
                        tokens.add(KtToken(KtTokenType.STRING_RAW, lineText.substring(i), lineIndex, startCol, i, len))
                        i = len
                        inRawString = true
                    }
                    continue
                } else {
                    // Regular string
                    val start = i
                    i++
                    var escaped = false
                    while (i < len) {
                        val ch = lineText[i]
                        if (escaped) {
                            escaped = false
                            i++
                        } else if (ch == '\\') {
                            escaped = true
                            i++
                        } else if (ch == '"') {
                            i++
                            break
                        } else {
                            i++
                        }
                    }
                    tokens.add(KtToken(KtTokenType.STRING_LITERAL, lineText.substring(start, i), lineIndex, startCol, start, i))
                    continue
                }
            }

            // 6. Char literal 'c'
            if (c == '\'') {
                val start = i
                i++
                if (i < len && lineText[i] == '\\') {
                    i += 2
                } else if (i < len) {
                    i++
                }
                if (i < len && lineText[i] == '\'') {
                    i++
                }
                tokens.add(KtToken(KtTokenType.CHAR_LITERAL, lineText.substring(start, i.coerceAtMost(len)), lineIndex, startCol, start, i.coerceAtMost(len)))
                continue
            }

            // 7. Annotations e.g. @Composable, @JvmStatic, @OptIn
            if (c == '@' && i + 1 < len && (lineText[i + 1].isLetter() || lineText[i + 1] == '_')) {
                val start = i
                i++
                while (i < len && (lineText[i].isLetterOrDigit() || lineText[i] == '_' || lineText[i] == '.' || lineText[i] == ':')) {
                    i++
                }
                tokens.add(KtToken(KtTokenType.ANNOTATION, lineText.substring(start, i), lineIndex, startCol, start, i))
                continue
            }

            // 8. Numbers (Hex, Binary, Float, Int)
            if (c.isDigit() || (c == '.' && i + 1 < len && lineText[i + 1].isDigit())) {
                val start = i
                if (c == '0' && i + 1 < len && (lineText[i + 1] == 'x' || lineText[i + 1] == 'X')) {
                    i += 2
                    while (i < len && (lineText[i].isDigit() || lineText[i] in 'a'..'f' || lineText[i] in 'A'..'F' || lineText[i] == '_')) {
                        i++
                    }
                    if (i < len && (lineText[i] == 'L' || lineText[i] == 'l')) i++
                    tokens.add(KtToken(KtTokenType.NUMBER_HEX, lineText.substring(start, i), lineIndex, startCol, start, i))
                    continue
                } else if (c == '0' && i + 1 < len && (lineText[i + 1] == 'b' || lineText[i + 1] == 'B')) {
                    i += 2
                    while (i < len && (lineText[i] == '0' || lineText[i] == '1' || lineText[i] == '_')) {
                        i++
                    }
                    if (i < len && (lineText[i] == 'L' || lineText[i] == 'l')) i++
                    tokens.add(KtToken(KtTokenType.NUMBER_BIN, lineText.substring(start, i), lineIndex, startCol, start, i))
                    continue
                } else {
                    var isFloat = false
                    while (i < len && (lineText[i].isDigit() || lineText[i] == '_')) i++
                    if (i < len && lineText[i] == '.' && (i + 1 >= len || lineText[i + 1] != '.')) {
                        isFloat = true
                        i++
                        while (i < len && (lineText[i].isDigit() || lineText[i] == '_')) i++
                    }
                    if (i < len && (lineText[i] == 'e' || lineText[i] == 'E')) {
                        isFloat = true
                        i++
                        if (i < len && (lineText[i] == '+' || lineText[i] == '-')) i++
                        while (i < len && lineText[i].isDigit()) i++
                    }
                    if (i < len && (lineText[i] == 'f' || lineText[i] == 'F')) {
                        isFloat = true
                        i++
                    } else if (i < len && (lineText[i] == 'L' || lineText[i] == 'l')) {
                        i++
                    }
                    val type = if (isFloat) KtTokenType.NUMBER_FLOAT else KtTokenType.NUMBER_INT
                    tokens.add(KtToken(type, lineText.substring(start, i), lineIndex, startCol, start, i))
                    continue
                }
            }

            // 9. Identifiers, Keywords, Builtin Types
            if (c.isLetter() || c == '_' || c == '`') {
                val start = i
                if (c == '`') {
                    i++
                    while (i < len && lineText[i] != '`') i++
                    if (i < len && lineText[i] == '`') i++
                    tokens.add(KtToken(KtTokenType.IDENTIFIER, lineText.substring(start, i), lineIndex, startCol, start, i))
                    continue
                } else {
                    while (i < len && (lineText[i].isLetterOrDigit() || lineText[i] == '_')) {
                        i++
                    }
                    val word = lineText.substring(start, i)
                    val tokenType = when {
                        KEYWORDS.containsKey(word) -> KEYWORDS[word]!!
                        BUILTIN_TYPES.containsKey(word) -> BUILTIN_TYPES[word]!!
                        else -> KtTokenType.IDENTIFIER
                    }
                    tokens.add(KtToken(tokenType, word, lineIndex, startCol, start, i))
                    continue
                }
            }

            // 10. Multi-char operators & delimiters
            val start = i
            val next1 = if (i + 1 < len) lineText[i + 1] else '\u0000'
            val next2 = if (i + 2 < len) lineText[i + 2] else '\u0000'

            when {
                c == '.' && next1 == '.' && next2 == '<' -> {
                    i += 3
                    tokens.add(KtToken(KtTokenType.RANGE_UNTIL, ".. <", lineIndex, startCol, start, i))
                }
                c == '.' && next1 == '.' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.RANGE, "..", lineIndex, startCol, start, i))
                }
                c == '?' && next1 == '.' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.DOT_SAFE, "?.", lineIndex, startCol, start, i))
                }
                c == '?' && next1 == ':' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.ELVIS, "?:", lineIndex, startCol, start, i))
                }
                c == ':' && next1 == ':' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.DOUBLE_COLON, "::", lineIndex, startCol, start, i))
                }
                c == '-' && next1 == '>' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.ARROW, "->", lineIndex, startCol, start, i))
                }
                c == '=' && next1 == '=' && next2 == '=' -> {
                    i += 3
                    tokens.add(KtToken(KtTokenType.EQ_EQ_EQ, "===", lineIndex, startCol, start, i))
                }
                c == '=' && next1 == '=' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.EQ_EQ, "==", lineIndex, startCol, start, i))
                }
                c == '!' && next1 == '=' && next2 == '=' -> {
                    i += 3
                    tokens.add(KtToken(KtTokenType.NOT_EQ_EQ, "!==", lineIndex, startCol, start, i))
                }
                c == '!' && next1 == '=' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.NOT_EQ, "!=", lineIndex, startCol, start, i))
                }
                c == '!' && next1 == '!' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.EXCL_EXCL, "!!", lineIndex, startCol, start, i))
                }
                c == '+' && next1 == '+' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.PLUS_PLUS, "++", lineIndex, startCol, start, i))
                }
                c == '-' && next1 == '-' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.MINUS_MINUS, "--", lineIndex, startCol, start, i))
                }
                c == '+' && next1 == '=' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.PLUS_EQ, "+=", lineIndex, startCol, start, i))
                }
                c == '-' && next1 == '=' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.MINUS_EQ, "-=", lineIndex, startCol, start, i))
                }
                c == '*' && next1 == '=' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.STAR_EQ, "*=", lineIndex, startCol, start, i))
                }
                c == '/' && next1 == '=' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.SLASH_EQ, "/=", lineIndex, startCol, start, i))
                }
                c == '<' && next1 == '=' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.LTE, "<=", lineIndex, startCol, start, i))
                }
                c == '>' && next1 == '=' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.GTE, ">=", lineIndex, startCol, start, i))
                }
                c == '&' && next1 == '&' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.AMP_AMP, "&&", lineIndex, startCol, start, i))
                }
                c == '|' && next1 == '|' -> {
                    i += 2
                    tokens.add(KtToken(KtTokenType.BAR_BAR, "||", lineIndex, startCol, start, i))
                }
                else -> {
                    i++
                    val singleType = when (c) {
                        '(' -> KtTokenType.LPAREN
                        ')' -> KtTokenType.RPAREN
                        '{' -> KtTokenType.LBRACE
                        '}' -> KtTokenType.RBRACE
                        '[' -> KtTokenType.LBRACKET
                        ']' -> KtTokenType.RBRACKET
                        ';' -> KtTokenType.SEMICOLON
                        ',' -> KtTokenType.COMMA
                        '.' -> KtTokenType.DOT
                        ':' -> KtTokenType.COLON
                        '=' -> KtTokenType.ASSIGN
                        '+' -> KtTokenType.PLUS
                        '-' -> KtTokenType.MINUS
                        '*' -> KtTokenType.STAR
                        '/' -> KtTokenType.SLASH
                        '%' -> KtTokenType.PERCENT
                        '<' -> KtTokenType.LT
                        '>' -> KtTokenType.GT
                        '?' -> KtTokenType.QUESTION
                        '!' -> KtTokenType.EXCL
                        else -> KtTokenType.UNKNOWN
                    }
                    tokens.add(KtToken(singleType, c.toString(), lineIndex, startCol, start, i))
                }
            }
        }

        return Pair(tokens, LexerState(inBlockComment, inRawString))
    }

    fun tokenizeEntireSource(sourceCode: String): List<KtToken> {
        val allTokens = mutableListOf<KtToken>()
        var state = LexerState()
        var offset = 0
        val lines = sourceCode.lines()
        for ((lineIdx, line) in lines.withIndex()) {
            val (lineTokens, nextState) = tokenizeLine(line, state, lineIdx)
            for (t in lineTokens) {
                allTokens.add(
                    KtToken(
                        type = t.type,
                        text = t.text,
                        line = lineIdx,
                        column = t.column,
                        startOffset = offset + t.startOffset,
                        endOffset = offset + t.endOffset
                    )
                )
            }
            state = nextState
            offset += line.length + 1 // + newline character
        }
        return allTokens
    }
}
