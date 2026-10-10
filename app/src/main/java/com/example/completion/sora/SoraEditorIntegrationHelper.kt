package com.example.completion.sora

import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.inputmethod.EditorInfo

import com.example.completion.core.CompletionRequest
import com.example.completion.project.KotlinCompletionFacade

/**
 * Coordinates Sora Editor lifecycle, typing debounce, keyboard input type flags,
 * and completion popup dispatching.
 */
class SoraEditorIntegrationHelper(val facade: KotlinCompletionFacade) {

    fun interface CompletionCallback {
        fun onCompletionsReady(items: List<SoraCompletionItem>, prefix: String)
    }

    val adapter: SoraCompletionAdapter = SoraCompletionAdapter()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var pendingRequestRunnable: Runnable? = null
    var debounceMillis: Long = 150

    /**
     * Configures the IME keyboard flags for Sora Editor, including password / coding keyboard flags
     * to prevent unwanted auto-correction and enable code completion keyboard symbols.
     */
    fun configureEditorKeyboard(editorInfo: EditorInfo?, isPasswordFlagEnabled: Boolean) {
        if (editorInfo == null) return

        if (isPasswordFlagEnabled) {
            // Password flag explicitly requested for coding/symbol security input
            editorInfo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        } else {
            editorInfo.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        editorInfo.imeOptions = EditorInfo.IME_ACTION_NONE or EditorInfo.IME_FLAG_NO_FULLSCREEN
    }

    /**
     * Debounced request for completion items triggered as the user types in the editor.
     */
    fun requestCompletion(filePath: String, content: String, cursorPosition: Int, callback: CompletionCallback?) {
        pendingRequestRunnable?.let { mainHandler.removeCallbacks(it) }

        val runnable = Runnable {
            val request = CompletionRequest.create(content, cursorPosition, filePath)
            facade.completeAsync(request).thenAccept { result ->
                val items = adapter.adapt(result)
                mainHandler.post {
                    callback?.onCompletionsReady(items, request.prefix)
                }
            }
        }
        pendingRequestRunnable = runnable

        mainHandler.postDelayed(runnable, debounceMillis)
    }
}
