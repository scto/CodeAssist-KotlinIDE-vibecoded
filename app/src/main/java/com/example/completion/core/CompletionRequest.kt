package com.example.completion.core

data class CompletionRequest(
    val content: String,
    val cursorPosition: Int,
    val filePath: String = "Untitled.kt",
    val prefix: String = extractPrefix(content, cursorPosition.coerceIn(0, content.length)),
    val line: Int = calculateLine(content, cursorPosition.coerceIn(0, content.length)),
    val column: Int = calculateColumn(content, cursorPosition.coerceIn(0, content.length)),
    val extraAttributes: Map<String, Any> = emptyMap()
) {
    init {
        require(cursorPosition in 0..content.length) { "cursorPosition out of bounds" }
    }

    fun getAttribute(key: String): Any? = extraAttributes[key]

    companion object {
        fun create(content: String, cursorPosition: Int, filePath: String = "Untitled.kt"): CompletionRequest {
            return CompletionRequest(content = content, cursorPosition = cursorPosition, filePath = filePath)
        }

        private fun extractPrefix(text: String, offset: Int): String {
            if (offset <= 0 || offset > text.length) return ""
            var start = offset
            while (start > 0) {
                val c = text[start - 1]
                if (Character.isJavaIdentifierPart(c)) {
                    start--
                } else {
                    break
                }
            }
            return text.substring(start, offset)
        }

        private fun calculateLine(text: String, offset: Int): Int {
            val max = offset.coerceAtMost(text.length)
            var line = 0
            for (i in 0 until max) {
                if (text[i] == '\n') line++
            }
            return line
        }

        private fun calculateColumn(text: String, offset: Int): Int {
            val max = offset.coerceAtMost(text.length)
            val lastNewLine = text.lastIndexOf('\n', max - 1)
            return if (lastNewLine == -1) max else max - (lastNewLine + 1)
        }
    }
}
