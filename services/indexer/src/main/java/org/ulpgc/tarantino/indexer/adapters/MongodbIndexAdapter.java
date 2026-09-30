package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

public class MongodbIndexAdapter implements InvertedIndexStorage {

    private final String connectionUri;

    public MongodbIndexAdapter(String connectionUri) {
        this.connectionUri = connectionUri;
    }

    @Override
    public void add(TermOccurrences occurrences) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void flush() {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
