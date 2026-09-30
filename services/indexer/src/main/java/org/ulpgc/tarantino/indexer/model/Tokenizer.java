package org.ulpgc.tarantino.indexer.model;

import java.util.Set;

/** Normalization rules must match the Python and C# implementations exactly. */
public class Tokenizer {

    private final Set<String> stopwords;

    public Tokenizer(Set<String> stopwords) {
        this.stopwords = stopwords;
    }

    public TermOccurrences tokenize(int bookId, String body) {
        // TODO: lowercase, strip non-letters, split on whitespace, drop stopwords, count terms
        throw new UnsupportedOperationException("TODO");
    }
}
