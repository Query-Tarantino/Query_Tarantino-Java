package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

/** Collection {@code tarantino.inverted_index}: {@code {"term": "...", "postings": [bookId, ...]}} */
public class MongodbIndexAdapter implements InvertedIndexStorage {

    private final String connectionUri;

    public MongodbIndexAdapter(String connectionUri) {
        this.connectionUri = connectionUri;
    }

    @Override
    public void add(TermOccurrences occurrences) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("TODO");
    }
}
