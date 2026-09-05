package com.example.completion.sora;

import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import com.example.completion.core.CompletionRequest;
import com.example.completion.core.CompletionResult;
import com.example.completion.project.KotlinCompletionFacade;

import java.util.List;

/**
 * Coordinates Sora Editor lifecycle, typing debounce, keyboard input type flags,
 * and completion popup dispatching.
 */
public class SoraEditorIntegrationHelper {

    public interface CompletionCallback {
        void onCompletionsReady(List<SoraCompletionItem> items, String prefix);
    }

    private final KotlinCompletionFacade facade;
    private final SoraCompletionAdapter adapter;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingRequestRunnable;
    private long debounceMillis = 150;

    public SoraEditorIntegrationHelper(KotlinCompletionFacade facade) {
        this.facade = facade;
        this.adapter = new SoraCompletionAdapter();
    }

    /**
     * Configures the IME keyboard flags for Sora Editor, including password / coding keyboard flags
     * to prevent unwanted auto-correction and enable code completion keyboard symbols.
     */
    public void configureEditorKeyboard(EditorInfo editorInfo, boolean isPasswordFlagEnabled) {
        if (editorInfo == null) return;

        if (isPasswordFlagEnabled) {
            // Password flag explicitly requested for coding/symbol security input
            editorInfo.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        } else {
            editorInfo.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS;
        }
        editorInfo.imeOptions = EditorInfo.IME_ACTION_NONE | EditorInfo.IME_FLAG_NO_FULLSCREEN;
    }

    /**
     * Debounced request for completion items triggered as the user types in the editor.
     */
    public void requestCompletion(String filePath, String content, int cursorPosition, CompletionCallback callback) {
        if (pendingRequestRunnable != null) {
            mainHandler.removeCallbacks(pendingRequestRunnable);
        }

        pendingRequestRunnable = () -> {
            CompletionRequest request = CompletionRequest.create(filePath, content, cursorPosition);
            facade.completeAsync(request).thenAccept(result -> {
                List<SoraCompletionItem> items = adapter.adapt(result);
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onCompletionsReady(items, request.getPrefix());
                    }
                });
            });
        };

        mainHandler.postDelayed(pendingRequestRunnable, debounceMillis);
    }

    public void setDebounceMillis(long debounceMillis) {
        this.debounceMillis = debounceMillis;
    }

    public SoraCompletionAdapter getAdapter() {
        return adapter;
    }

    public KotlinCompletionFacade getFacade() {
        return facade;
    }
}
