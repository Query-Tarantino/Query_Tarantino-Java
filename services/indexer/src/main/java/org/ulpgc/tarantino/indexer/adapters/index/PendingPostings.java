package org.ulpgc.tarantino.indexer.adapters.index;

import org.ulpgc.tarantino.indexer.model.terms.TermOccurrences;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class PendingPostings {

    private final Map<String, Set<Integer>> postings = new HashMap<>();

    public void add(TermOccurrences occurrences) {
        occurrences.frequencies().keySet()
                .forEach(term -> postings.computeIfAbsent(term, key -> new TreeSet<>()).add(occurrences.bookId()));
    }

    public Map<String, Set<Integer>> drain() {
        Map<String, Set<Integer>> drained = Map.copyOf(postings);
        postings.clear();
        return drained;
    }
}
