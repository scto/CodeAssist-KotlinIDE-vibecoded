package com.example.completion.core

import java.util.Locale

object CompletionScore {

    const val BASE_EXACT_MATCH = 10000
    const val BASE_PREFIX_MATCH = 5000
    const val BASE_CAMEL_CASE_MATCH = 3000
    const val BASE_SUBSTRING_MATCH = 1500
    const val BASE_FUZZY_MATCH = 800

    const val BONUS_LOCAL = 400
    const val BONUS_CURRENT_FILE = 300
    const val BONUS_SOURCE = 200
    const val BONUS_METADATA = 100
    const val BONUS_BUILTIN = 80
    const val BONUS_KEYWORD = 60
    const val BONUS_JAR = 40

    fun calculateScore(item: CompletionItem, prefix: String?): Int {
        if (prefix.isNullOrEmpty()) {
            return calculateOriginBonus(item.origin) + item.priority
        }

        val matchScore = computeTextMatchScore(item.label, prefix)
        if (matchScore <= 0) return 0 

        return matchScore + calculateOriginBonus(item.origin) + item.priority
    }

    fun computeTextMatchScore(label: String?, prefix: String?): Int {
        if (label.isNullOrEmpty()) return 0
        if (prefix.isNullOrEmpty()) return 100

        if (label == prefix) return BASE_EXACT_MATCH
        if (label.equals(prefix, ignoreCase = true)) return BASE_EXACT_MATCH - 200

        if (label.startsWith(prefix)) {
            val lengthDiff = label.length - prefix.length
            return BASE_PREFIX_MATCH - (lengthDiff * 5).coerceAtMost(1000)
        }

        val lowerLabel = label.lowercase(Locale.ROOT)
        val lowerPrefix = prefix.lowercase(Locale.ROOT)

        if (lowerLabel.startsWith(lowerPrefix)) {
            val lengthDiff = label.length - prefix.length
            return BASE_PREFIX_MATCH - 400 - (lengthDiff * 5).coerceAtMost(1000)
        }

        if (matchesCamelCase(label, prefix)) {
            return BASE_CAMEL_CASE_MATCH
        }

        val subIndex = lowerLabel.indexOf(lowerPrefix)
        if (subIndex > 0) {
            return BASE_SUBSTRING_MATCH - (subIndex * 50)
        }

        val fuzzyScore = calculateFuzzySubsequenceScore(lowerLabel, lowerPrefix)
        if (fuzzyScore > 0) {
            return BASE_FUZZY_MATCH + fuzzyScore
        }

        return 0
    }

    private fun matchesCamelCase(label: String, prefix: String): Boolean {
        if (prefix.length > label.length) return false
        var pIdx = 0
        val pLen = prefix.length

        for (i in label.indices) {
            if (pIdx >= pLen) break
            val c = label[i]
            val p = prefix[pIdx]

            if (c.uppercaseChar() == p.uppercaseChar()) {
                if (i == 0 || c.isUpperCase() || label.getOrNull(i - 1) == '_') {
                    pIdx++
                }
            }
        }
        return pIdx == pLen
    }

    private fun calculateFuzzySubsequenceScore(label: String, prefix: String): Int {
        var labelIdx = 0
        var prefixIdx = 0
        var consecutive = 0
        var bonus = 0

        while (labelIdx < label.length && prefixIdx < prefix.length) {
            if (label[labelIdx] == prefix[prefixIdx]) {
                consecutive++
                bonus += consecutive * 10
                prefixIdx++
            } else {
                consecutive = 0
            }
            labelIdx++
        }

        return if (prefixIdx == prefix.length) {
            10.coerceAtLeast(200 - (label.length - prefix.length) + bonus)
        } else {
            0
        }
    }

    fun calculateOriginBonus(origin: SymbolOrigin?): Int {
        return when (origin) {
            SymbolOrigin.LOCAL -> BONUS_LOCAL
            SymbolOrigin.SOURCE -> BONUS_SOURCE
            SymbolOrigin.KOTLIN_METADATA -> BONUS_METADATA
            SymbolOrigin.BUILTIN -> BONUS_BUILTIN
            SymbolOrigin.KEYWORD -> BONUS_KEYWORD
            SymbolOrigin.JAR -> BONUS_JAR
            else -> 0
        }
    }
}
