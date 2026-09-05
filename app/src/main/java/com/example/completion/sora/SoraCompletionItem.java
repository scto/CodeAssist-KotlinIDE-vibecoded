package com.example.completion.sora;

import com.example.completion.core.CompletionItem;
import com.example.completion.core.SymbolKind;

/**
 * Adapter item holding metadata for display and insertion inside Sora Editor.
 */
public class SoraCompletionItem {

    private final CompletionItem coreItem;

    public SoraCompletionItem(CompletionItem coreItem) {
        this.coreItem = coreItem;
    }

    public CompletionItem getCoreItem() {
        return coreItem;
    }

    public String getLabel() {
        return coreItem != null ? coreItem.getLabel() : "";
    }

    public String getInsertText() {
        return coreItem != null ? coreItem.getInsertText() : "";
    }

    public String getDetail() {
        return coreItem != null ? coreItem.getDetail() : "";
    }

    public SymbolKind getKind() {
        return coreItem != null ? coreItem.getKind() : SymbolKind.VARIABLE;
    }

    public String getImportToInsert() {
        return coreItem != null ? coreItem.getImportToInsert() : null;
    }

    @Override
    public String toString() {
        return getLabel() + " (" + getDetail() + ")";
    }
}
