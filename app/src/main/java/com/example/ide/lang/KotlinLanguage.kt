package com.example.ide.lang

import android.os.Bundle
import com.example.ide.completion.KotlinCompletionProvider
import com.example.ide.psi.KtLexer
import com.example.ide.psi.KtToken
import com.example.ide.psi.KtTokenType
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.AsyncIncrementalAnalyzeManager
import io.github.rosemoe.sora.lang.analysis.IncrementalAnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lang.styling.CodeBlock
import io.github.rosemoe.sora.lang.styling.Span
import io.github.rosemoe.sora.lang.styling.TextStyle
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

/**
 * Full Kotlin Language implementation for Sora CodeEditor.
 * Provides real-time incremental syntax highlighting, block matching, smart auto-indent, and PSI auto-completion.
 */
class KotlinLanguage(
    private val completionProvider: KotlinCompletionProvider = KotlinCompletionProvider()
) : EmptyLanguage() {

    private val lexer = KtLexer()

    private val analyzeManager = object : AsyncIncrementalAnalyzeManager<KtLexer.LexerState, KtToken>() {
        override fun getInitialState(): KtLexer.LexerState = KtLexer.LexerState()

        override fun stateEquals(s1: KtLexer.LexerState?, s2: KtLexer.LexerState?): Boolean = s1 == s2

        override fun tokenizeLine(
            line: CharSequence?,
            state: KtLexer.LexerState?,
            lineIndex: Int
        ): IncrementalAnalyzeManager.LineTokenizeResult<KtLexer.LexerState, KtToken> {
            val text = line ?: ""
            val (tokens, nextState) = lexer.tokenizeLine(text, state ?: KtLexer.LexerState(), lineIndex)
            val spans = mutableListOf<Span>()

            for (t in tokens) {
                if (t.type == KtTokenType.WHITESPACE) continue
                val colorId = getColorIdForTokenType(t.type)
                spans.add(Span.obtain(t.column, TextStyle.makeStyle(colorId)))
            }

            if (spans.isEmpty()) {
                spans.add(Span.obtain(0, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)))
            }

            return IncrementalAnalyzeManager.LineTokenizeResult(nextState, tokens.toMutableList(), spans)
        }

        override fun generateSpansForLine(
            result: IncrementalAnalyzeManager.LineTokenizeResult<KtLexer.LexerState, KtToken>?
        ): MutableList<Span> {
            val spans = ArrayList<Span>()
            if (result != null && result.tokens != null) {
                for (t in result.tokens) {
                    if (t.type == KtTokenType.WHITESPACE) continue
                    val colorId = getColorIdForTokenType(t.type)
                    spans.add(Span.obtain(t.column, TextStyle.makeStyle(colorId)))
                }
            }
            if (spans.isEmpty()) {
                spans.add(Span.obtain(0, TextStyle.makeStyle(EditorColorScheme.TEXT_NORMAL)))
            }
            return spans
        }

        override fun computeBlocks(
            content: Content?,
            delegate: CodeBlockAnalyzeDelegate?
        ): MutableList<CodeBlock>? {
            if (content == null) return null
            val blocks = mutableListOf<CodeBlock>()
            val stack = mutableListOf<CodeBlock>()

            for (lineIdx in 0 until content.lineCount) {
                val line = content.getLine(lineIdx)
                for (colIdx in 0 until line.length) {
                    val c = line[colIdx]
                    if (c == '{') {
                        val block = CodeBlock()
                        block.startLine = lineIdx
                        block.startColumn = colIdx
                        stack.add(block)
                    } else if (c == '}' && stack.isNotEmpty()) {
                        val block = stack.removeAt(stack.lastIndex)
                        block.endLine = lineIdx
                        block.endColumn = colIdx
                        if (block.endLine > block.startLine) {
                            blocks.add(block)
                        }
                    }
                }
            }
            return blocks
        }
    }

    private fun getColorIdForTokenType(type: KtTokenType): Int {
        return when (type) {
            KtTokenType.KEYWORD_PACKAGE,
            KtTokenType.KEYWORD_IMPORT,
            KtTokenType.KEYWORD_FUN,
            KtTokenType.KEYWORD_VAL,
            KtTokenType.KEYWORD_VAR,
            KtTokenType.KEYWORD_CLASS,
            KtTokenType.KEYWORD_INTERFACE,
            KtTokenType.KEYWORD_OBJECT,
            KtTokenType.KEYWORD_COMPANION,
            KtTokenType.KEYWORD_DATA,
            KtTokenType.KEYWORD_ENUM,
            KtTokenType.KEYWORD_SEALED,
            KtTokenType.KEYWORD_OPEN,
            KtTokenType.KEYWORD_OVERRIDE,
            KtTokenType.KEYWORD_PRIVATE,
            KtTokenType.KEYWORD_PROTECTED,
            KtTokenType.KEYWORD_PUBLIC,
            KtTokenType.KEYWORD_INTERNAL,
            KtTokenType.KEYWORD_ABSTRACT,
            KtTokenType.KEYWORD_FINAL,
            KtTokenType.KEYWORD_INLINE,
            KtTokenType.KEYWORD_TAILREC,
            KtTokenType.KEYWORD_SUSPEND,
            KtTokenType.KEYWORD_OPERATOR,
            KtTokenType.KEYWORD_INFIX,
            KtTokenType.KEYWORD_CONSTRUCTOR,
            KtTokenType.KEYWORD_INIT,
            KtTokenType.KEYWORD_THIS,
            KtTokenType.KEYWORD_SUPER,
            KtTokenType.KEYWORD_IF,
            KtTokenType.KEYWORD_ELSE,
            KtTokenType.KEYWORD_WHEN,
            KtTokenType.KEYWORD_TRY,
            KtTokenType.KEYWORD_CATCH,
            KtTokenType.KEYWORD_FINALLY,
            KtTokenType.KEYWORD_FOR,
            KtTokenType.KEYWORD_DO,
            KtTokenType.KEYWORD_WHILE,
            KtTokenType.KEYWORD_BREAK,
            KtTokenType.KEYWORD_CONTINUE,
            KtTokenType.KEYWORD_RETURN,
            KtTokenType.KEYWORD_THROW,
            KtTokenType.KEYWORD_IS,
            KtTokenType.KEYWORD_NOT_IS,
            KtTokenType.KEYWORD_AS,
            KtTokenType.KEYWORD_AS_SAFE,
            KtTokenType.KEYWORD_IN,
            KtTokenType.KEYWORD_NOT_IN,
            KtTokenType.KEYWORD_BY,
            KtTokenType.KEYWORD_TYPEALIAS,
            KtTokenType.KEYWORD_WHERE,
            KtTokenType.KEYWORD_GET,
            KtTokenType.KEYWORD_SET,
            KtTokenType.KEYWORD_VARARG,
            KtTokenType.KEYWORD_NOINLINE,
            KtTokenType.KEYWORD_CROSSINLINE,
            KtTokenType.KEYWORD_REIFIED,
            KtTokenType.KEYWORD_CONST,
            KtTokenType.KEYWORD_LATEINIT,
            KtTokenType.KEYWORD_DYNAMIC -> EditorColorScheme.KEYWORD

            KtTokenType.TYPE_INT,
            KtTokenType.TYPE_LONG,
            KtTokenType.TYPE_SHORT,
            KtTokenType.TYPE_BYTE,
            KtTokenType.TYPE_FLOAT,
            KtTokenType.TYPE_DOUBLE,
            KtTokenType.TYPE_BOOLEAN,
            KtTokenType.TYPE_CHAR,
            KtTokenType.TYPE_STRING,
            KtTokenType.TYPE_ARRAY,
            KtTokenType.TYPE_LIST,
            KtTokenType.TYPE_MAP,
            KtTokenType.TYPE_SET,
            KtTokenType.TYPE_UNIT,
            KtTokenType.TYPE_NOTHING,
            KtTokenType.TYPE_ANY -> EditorColorScheme.KEYWORD

            KtTokenType.STRING_LITERAL,
            KtTokenType.STRING_RAW,
            KtTokenType.CHAR_LITERAL -> EditorColorScheme.LITERAL

            KtTokenType.NUMBER_INT,
            KtTokenType.NUMBER_FLOAT,
            KtTokenType.NUMBER_HEX,
            KtTokenType.NUMBER_BIN,
            KtTokenType.BOOLEAN_TRUE,
            KtTokenType.BOOLEAN_FALSE,
            KtTokenType.NULL_LITERAL -> EditorColorScheme.LITERAL

            KtTokenType.COMMENT_LINE,
            KtTokenType.COMMENT_BLOCK,
            KtTokenType.COMMENT_DOC -> EditorColorScheme.COMMENT

            KtTokenType.ANNOTATION -> EditorColorScheme.OPERATOR

            KtTokenType.PLUS,
            KtTokenType.MINUS,
            KtTokenType.STAR,
            KtTokenType.SLASH,
            KtTokenType.PERCENT,
            KtTokenType.ASSIGN,
            KtTokenType.EQ_EQ,
            KtTokenType.EQ_EQ_EQ,
            KtTokenType.NOT_EQ,
            KtTokenType.ARROW,
            KtTokenType.ELVIS,
            KtTokenType.DOT_SAFE -> EditorColorScheme.OPERATOR

            else -> EditorColorScheme.TEXT_NORMAL
        }
    }

    override fun getAnalyzeManager(): AnalyzeManager = analyzeManager

    override fun getInterruptionLevel(): Int = Language.INTERRUPTION_LEVEL_STRONG

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        completionProvider.complete(content, position, publisher)
    }

    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        val lineText = content.getLine(line).toString().trimEnd()
        return if (lineText.endsWith("{") || lineText.endsWith("(") || lineText.endsWith("->")) 4 else 0
    }

    override fun useTab(): Boolean = false

    override fun getSymbolPairs(): SymbolPairMatch? {
        return null
    }
}
