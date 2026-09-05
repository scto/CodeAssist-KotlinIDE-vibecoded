package com.example.completion.sora;

import com.example.completion.core.CompletionItem;
import com.example.completion.core.CompletionResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter converting engine CompletionResult models into Sora Editor completion structures.
 */
public class SoraCompletionAdapter {

    public List<SoraCompletionItem> adapt(CompletionResult result) {
        if (result == null || result.getItems().isEmpty()) {
            return new ArrayList<>();
        }

        List<SoraCompletionItem> list = new ArrayList<>(result.getItems().size());
        for (CompletionItem item : result.getItems()) {
            list.add(new SoraCompletionItem(item));
        }
        return list;
    }

    /**
     * Injects auto-import directive into the source code if needed.
     */
    public String insertImportIfNeeded(String currentSource, String importToInsert) {
        if (currentSource == null || importToInsert == null || importToInsert.isEmpty()) {
            return currentSource;
        }

        String importStatement = "import " + importToInsert;
        if (currentSource.contains(importStatement)) {
            return currentSource; // Already imported
        }

        int packageIdx = currentSource.indexOf("package ");
        if (packageIdx != -1) {
            int lineEnd = currentSource.indexOf('\n', packageIdx);
            if (lineEnd != -1) {
                return currentSource.substring(0, lineEnd + 1) + importStatement + "\n" + currentSource.substring(lineEnd + 1);
            }
        }

        // Prepend to top of file
        return importStatement + "\n" + currentSource;
    }
}
