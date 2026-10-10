package com.example.completion.sora

import com.example.completion.core.CompletionResult

/**
 * Adapter converting engine CompletionResult models into Sora Editor completion structures.
 */
class SoraCompletionAdapter {

    fun adapt(result: CompletionResult?): List<SoraCompletionItem> {
        if (result == null || result.items.isEmpty()) {
            return ArrayList()
        }

        val list = ArrayList<SoraCompletionItem>(result.items.size)
        for (item in result.items) {
            list.add(SoraCompletionItem(item))
        }
        return list
    }

    /**
     * Injects auto-import directive into the source code if needed.
     */
    fun insertImportIfNeeded(currentSource: String?, importToInsert: String?): String? {
        if (currentSource == null || importToInsert == null || importToInsert.isEmpty()) {
            return currentSource
        }

        val importStatement = "import $importToInsert"
        if (currentSource.contains(importStatement)) {
            return currentSource // Already imported
        }

        val packageIdx = currentSource.indexOf("package ")
        if (packageIdx != -1) {
            val lineEnd = currentSource.indexOf('\n', packageIdx)
            if (lineEnd != -1) {
                return currentSource.substring(0, lineEnd + 1) + importStatement + "\n" + currentSource.substring(lineEnd + 1)
            }
        }

        // Prepend to top of file
        return "$importStatement\n$currentSource"
    }
}
