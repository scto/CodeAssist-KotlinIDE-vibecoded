package com.example.ide.completion

import com.example.completion.core.CompletionRequest
import com.example.completion.core.CompletionResult
import com.example.completion.project.KotlinCompletionFacade
import com.example.completion.sora.SoraCompletionAdapter
import com.example.completion.sora.SoraCompletionItem
import com.example.ide.analysis.WorkspaceIndex
import com.example.ide.psi.KtParser
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference

/**
 * Intelligent Kotlin Code Completion Provider powered by the Kotlin Completion Engine.
 */
class KotlinCompletionProvider(
    private val workspaceIndex: WorkspaceIndex? = null,
    private val parser: KtParser = KtParser(),
    val facade: KotlinCompletionFacade = KotlinCompletionFacade.createDefault()
) {

    private val soraAdapter = SoraCompletionAdapter()

    fun complete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher
    ) {
        val lineIndex = position.line
        val columnIndex = position.column
        if (lineIndex < 0 || lineIndex >= content.lineCount) return

        val entireSource = content.toString()
        val lineText = content.getLine(lineIndex).toString()
        val safeCol = columnIndex.coerceIn(0, lineText.length)

        // Calculate absolute cursor position in characters
        var charOffset = 0
        for (i in 0 until lineIndex) {
            charOffset += content.getLine(i).length + 1
        }
        charOffset += safeCol

        val request = CompletionRequest.create(entireSource, charOffset, "ActiveFile.kt")
        val result: CompletionResult = facade.complete(request)
        val soraItems: List<SoraCompletionItem> = soraAdapter.adapt(result)

        val prefixLen = request.prefix.length

        for (item in soraItems) {
            val label = item.label
            val detail = item.detail
            val insert = item.insertText

            val soraItem = SimpleCompletionItem(
                label,
                if (detail.isNotEmpty()) "$label — $detail" else label,
                prefixLen,
                insert
            )
            publisher.addItem(soraItem)
        }
    }
}
