package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.TermOccurrences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class PendingPostings {

    private final Map<String, Set<Integer>> postings = new HashMap<>();

    void add(TermOccurrences occurrences) {
        occurrences.frequencies().keySet()
                .forEach(term -> postings.computeIfAbsent(term, key -> new TreeSet<>()).add(occurrences.bookId()));
    }

    Map<String, Set<Integer>> drain() {
        Map<String, Set<Integer>> drained = Map.copyOf(postings);
        postings.clear();
        return drained;
    }
}
