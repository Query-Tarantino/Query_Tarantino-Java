package org.ulpgc.tarantino.query.adapters;

import org.ulpgc.tarantino.query.ports.InvertedIndexReader;

import java.nio.file.Path;
import java.util.Set;

/** Reads {@code datamarts/inverted_index.json} */
public class MonolithicJsonIndexReader implements InvertedIndexReader {

    private final Path file;

    public MonolithicJsonIndexReader(Path file) {
        this.file = file;
    }

    @Override
    public Set<Integer> postings(String term) {
        throw new UnsupportedOperationException("TODO");
    }
}
