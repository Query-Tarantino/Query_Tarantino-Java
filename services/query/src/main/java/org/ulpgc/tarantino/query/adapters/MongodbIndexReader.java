package org.ulpgc.tarantino.query.adapters;

import org.ulpgc.tarantino.query.ports.InvertedIndexReader;

import java.util.Set;

public class MongodbIndexReader implements InvertedIndexReader {

    private final String connectionUri;

    public MongodbIndexReader(String connectionUri) {
        this.connectionUri = connectionUri;
    }

    @Override
    public Set<Integer> postings(String term) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
