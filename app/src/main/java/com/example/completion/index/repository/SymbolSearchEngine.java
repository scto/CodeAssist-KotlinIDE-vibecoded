package com.example.completion.index.repository;

import com.example.completion.core.CompletionScore;
import com.example.completion.symbol.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * High-performance search and matching engine over symbol repositories.
 */
public class SymbolSearchEngine {

    private final SymbolRepository symbolRepository;

    public SymbolSearchEngine(SymbolRepository symbolRepository) {
        this.symbolRepository = symbolRepository;
    }

    public List<Symbol> search(String query, int limit) {
        if (symbolRepository == null) return Collections.emptyList();
        List<Symbol> all = symbolRepository.search(query);

        if (query == null || query.isEmpty()) {
            return all.size() > limit ? new ArrayList<>(all.subList(0, limit)) : all;
        }

        List<ScoredSymbol> scored = new ArrayList<>();
        for (Symbol s : all) {
            int score = CompletionScore.computeTextMatchScore(s.getName(), query) + CompletionScore.calculateOriginBonus(s.getOrigin());
            if (score > 0) {
                scored.add(new ScoredSymbol(s, score));
            }
        }

        scored.sort((a, b) -> Integer.compare(b.score, a.score));

        List<Symbol> results = new ArrayList<>();
        int count = Math.min(scored.size(), limit);
        for (int i = 0; i < count; i++) {
            results.add(scored.get(i).symbol);
        }
        return results;
    }

    private static class ScoredSymbol {
        final Symbol symbol;
        final int score;

        ScoredSymbol(Symbol symbol, int score) {
            this.symbol = symbol;
            this.score = score;
        }
    }
}
