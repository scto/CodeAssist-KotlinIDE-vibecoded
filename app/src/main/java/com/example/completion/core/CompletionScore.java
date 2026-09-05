package com.example.completion.core;

import java.util.Locale;

/**
 * Scoring and ranking calculator for completion items.
 * Implements exact match, prefix match, camel-case matching, fuzzy matching, and origin priority.
 */
public final class CompletionScore {

    public static final int BASE_EXACT_MATCH = 10000;
    public static final int BASE_PREFIX_MATCH = 5000;
    public static final int BASE_CAMEL_CASE_MATCH = 3000;
    public static final int BASE_SUBSTRING_MATCH = 1500;
    public static final int BASE_FUZZY_MATCH = 800;

    // Origin priority bonuses
    public static final int BONUS_LOCAL = 400;
    public static final int BONUS_CURRENT_FILE = 300;
    public static final int BONUS_SOURCE = 200;
    public static final int BONUS_METADATA = 100;
    public static final int BONUS_BUILTIN = 80;
    public static final int BONUS_KEYWORD = 60;
    public static final int BONUS_JAR = 40;

    private CompletionScore() {}

    /**
     * Computes the relevance score of a completion item against the given query prefix.
     * Higher score indicates higher ranking. Returns 0 if it does not match at all.
     */
    public static int calculateScore(CompletionItem item, String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return calculateOriginBonus(item.getOrigin()) + item.getPriority();
        }

        String label = item.getLabel();
        int matchScore = computeTextMatchScore(label, prefix);
        if (matchScore <= 0) {
            return 0; // Filtered out
        }

        int originBonus = calculateOriginBonus(item.getOrigin());
        return matchScore + originBonus + item.getPriority();
    }

    public static int computeTextMatchScore(String label, String prefix) {
        if (label == null || label.isEmpty()) return 0;
        if (prefix == null || prefix.isEmpty()) return 100;

        // 1. Exact match
        if (label.equals(prefix)) {
            return BASE_EXACT_MATCH;
        }

        // 2. Exact case-insensitive match
        if (label.equalsIgnoreCase(prefix)) {
            return BASE_EXACT_MATCH - 200;
        }

        // 3. Prefix match (Case sensitive)
        if (label.startsWith(prefix)) {
            int lengthDiff = label.length() - prefix.length();
            return BASE_PREFIX_MATCH - Math.min(lengthDiff * 5, 1000);
        }

        // 4. Prefix match (Case insensitive)
        String lowerLabel = label.toLowerCase(Locale.ROOT);
        String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
        if (lowerLabel.startsWith(lowerPrefix)) {
            int lengthDiff = label.length() - prefix.length();
            return BASE_PREFIX_MATCH - 400 - Math.min(lengthDiff * 5, 1000);
        }

        // 5. CamelCase / Acronym match (e.g. "AL" -> "ArrayList", "NPE" -> "NullPointerException")
        if (matchesCamelCase(label, prefix)) {
            return BASE_CAMEL_CASE_MATCH;
        }

        // 6. Substring match
        int subIndex = lowerLabel.indexOf(lowerPrefix);
        if (subIndex > 0) {
            return BASE_SUBSTRING_MATCH - (subIndex * 50);
        }

        // 7. Fuzzy match (Subsequence)
        int fuzzyScore = calculateFuzzySubsequenceScore(lowerLabel, lowerPrefix);
        if (fuzzyScore > 0) {
            return BASE_FUZZY_MATCH + fuzzyScore;
        }

        return 0;
    }

    private static boolean matchesCamelCase(String label, String prefix) {
        if (prefix.length() > label.length()) return false;
        int pIdx = 0;
        int pLen = prefix.length();

        for (int i = 0; i < label.length() && pIdx < pLen; i++) {
            char c = label.charAt(i);
            char p = prefix.charAt(pIdx);

            if (Character.toUpperCase(c) == Character.toUpperCase(p)) {
                if (i == 0 || Character.isUpperCase(c) || label.charAt(i - 1) == '_') {
                    pIdx++;
                }
            }
        }
        return pIdx == pLen;
    }

    private static int calculateFuzzySubsequenceScore(String label, String prefix) {
        int labelIdx = 0;
        int prefixIdx = 0;
        int matches = 0;
        int consecutive = 0;
        int bonus = 0;

        while (labelIdx < label.length() && prefixIdx < prefix.length()) {
            if (label.charAt(labelIdx) == prefix.charAt(prefixIdx)) {
                matches++;
                consecutive++;
                bonus += (consecutive * 10);
                prefixIdx++;
            } else {
                consecutive = 0;
            }
            labelIdx++;
        }

        if (prefixIdx == prefix.length()) {
            return Math.max(10, 200 - (label.length() - prefix.length()) + bonus);
        }
        return 0;
    }

    public static int calculateOriginBonus(SymbolOrigin origin) {
        if (origin == null) return 0;
        switch (origin) {
            case LOCAL:
                return BONUS_LOCAL;
            case SOURCE:
                return BONUS_SOURCE;
            case KOTLIN_METADATA:
                return BONUS_METADATA;
            case BUILTIN:
                return BONUS_BUILTIN;
            case KEYWORD:
                return BONUS_KEYWORD;
            case JAR:
                return BONUS_JAR;
            default:
                return 0;
        }
    }
}
