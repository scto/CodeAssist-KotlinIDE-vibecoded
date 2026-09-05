package com.example.ide.psi

enum class KtTokenType {
    // Keywords
    KEYWORD_PACKAGE,
    KEYWORD_IMPORT,
    KEYWORD_FUN,
    KEYWORD_VAL,
    KEYWORD_VAR,
    KEYWORD_CLASS,
    KEYWORD_INTERFACE,
    KEYWORD_OBJECT,
    KEYWORD_COMPANION,
    KEYWORD_DATA,
    KEYWORD_ENUM,
    KEYWORD_SEALED,
    KEYWORD_OPEN,
    KEYWORD_OVERRIDE,
    KEYWORD_PRIVATE,
    KEYWORD_PROTECTED,
    KEYWORD_PUBLIC,
    KEYWORD_INTERNAL,
    KEYWORD_ABSTRACT,
    KEYWORD_FINAL,
    KEYWORD_INLINE,
    KEYWORD_TAILREC,
    KEYWORD_SUSPEND,
    KEYWORD_OPERATOR,
    KEYWORD_INFIX,
    KEYWORD_CONSTRUCTOR,
    KEYWORD_INIT,
    KEYWORD_THIS,
    KEYWORD_SUPER,
    KEYWORD_IF,
    KEYWORD_ELSE,
    KEYWORD_WHEN,
    KEYWORD_TRY,
    KEYWORD_CATCH,
    KEYWORD_FINALLY,
    KEYWORD_FOR,
    KEYWORD_DO,
    KEYWORD_WHILE,
    KEYWORD_BREAK,
    KEYWORD_CONTINUE,
    KEYWORD_RETURN,
    KEYWORD_THROW,
    KEYWORD_IS,
    KEYWORD_NOT_IS,
    KEYWORD_AS,
    KEYWORD_AS_SAFE,
    KEYWORD_IN,
    KEYWORD_NOT_IN,
    KEYWORD_BY,
    KEYWORD_TYPEALIAS,
    KEYWORD_WHERE,
    KEYWORD_GET,
    KEYWORD_SET,
    KEYWORD_VARARG,
    KEYWORD_NOINLINE,
    KEYWORD_CROSSINLINE,
    KEYWORD_REIFIED,
    KEYWORD_CONST,
    KEYWORD_LATEINIT,
    KEYWORD_DYNAMIC,

    // Built-in Types
    TYPE_INT,
    TYPE_LONG,
    TYPE_SHORT,
    TYPE_BYTE,
    TYPE_FLOAT,
    TYPE_DOUBLE,
    TYPE_BOOLEAN,
    TYPE_CHAR,
    TYPE_STRING,
    TYPE_ARRAY,
    TYPE_LIST,
    TYPE_MAP,
    TYPE_SET,
    TYPE_UNIT,
    TYPE_NOTHING,
    TYPE_ANY,

    // Literals & Identifiers
    IDENTIFIER,
    NUMBER_INT,
    NUMBER_FLOAT,
    NUMBER_HEX,
    NUMBER_BIN,
    STRING_LITERAL,
    STRING_RAW,
    CHAR_LITERAL,
    BOOLEAN_TRUE,
    BOOLEAN_FALSE,
    NULL_LITERAL,

    // Comments
    COMMENT_LINE,
    COMMENT_BLOCK,
    COMMENT_DOC,

    // Annotations
    ANNOTATION,

    // Delimiters and Operators
    LPAREN,      // (
    RPAREN,      // )
    LBRACE,      // {
    RBRACE,      // }
    LBRACKET,    // [
    RBRACKET,    // ]
    SEMICOLON,   // ;
    COMMA,       // ,
    DOT,         // .
    DOT_SAFE,    // ?.
    DOUBLE_COLON,// ::
    COLON,       // :
    ARROW,       // ->
    DOUBLE_ARROW,// =>
    ASSIGN,      // =
    EQ_EQ,       // ==
    EQ_EQ_EQ,    // ===
    NOT_EQ,      // !=
    NOT_EQ_EQ,   // !==
    EXCL,        // !
    EXCL_EXCL,   // !!
    PLUS,        // +
    MINUS,       // -
    STAR,        // *
    SLASH,       // /
    PERCENT,     // %
    PLUS_EQ,     // +=
    MINUS_EQ,    // -=
    STAR_EQ,     // *=
    SLASH_EQ,    // /=
    PERCENT_EQ,  // %=
    PLUS_PLUS,   // ++
    MINUS_MINUS, // --
    ELVIS,       // ?:
    RANGE,       // ..
    RANGE_UNTIL, // ..<
    LT,          // <
    GT,          // >
    LTE,         // <=
    GTE,         // >=
    AMP_AMP,     // &&
    BAR_BAR,     // ||
    QUESTION,    // ?

    // Whitespace / EOF / Error
    WHITESPACE,
    NEWLINE,
    UNKNOWN,
    EOF
}

data class KtToken(
    val type: KtTokenType,
    val text: String,
    val line: Int,
    val column: Int,
    val startOffset: Int,
    val endOffset: Int
) {
    val length: Int get() = endOffset - startOffset
}
