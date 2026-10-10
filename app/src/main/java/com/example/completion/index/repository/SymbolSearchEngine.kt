package com.example.completion.index.repository

import com.example.completion.core.CompletionScore
import com.example.completion.symbol.Symbol

import java.util.Collections

/**
 * High-performance search and matching engine over symbol repositories.
 */
class SymbolSearchEngine(private val symbolRepository: SymbolRepository?) {

    fun search(query: String?, limit: Int): List<Symbol> {
        if (symbolRepository == null) return Collections.emptyList()
        val all = symbolRepository.search(query)

        if (query == null || query.isEmpty()) {
            return if (all.size > limit) ArrayList(all.subList(0, limit)) else all
        }

        val scored = ArrayList<ScoredSymbol>()
        for (s in all) {
            val score = CompletionScore.computeTextMatchScore(s.name, query) + CompletionScore.calculateOriginBonus(s.origin)
            if (score > 0) {
                scored.add(ScoredSymbol(s, score))
            }
        }

        scored.sortWith { a, b -> b.score.compareTo(a.score) }

        val results = ArrayList<Symbol>()
        val count = Math.min(scored.size, limit)
        for (i in 0 until count) {
            results.add(scored[i].symbol)
        }
        return results
    }

    private class ScoredSymbol(val symbol: Symbol, val score: Int)
}
