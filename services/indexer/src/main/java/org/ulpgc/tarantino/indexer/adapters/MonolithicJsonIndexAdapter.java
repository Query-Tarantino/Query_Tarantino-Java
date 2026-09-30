package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

import java.nio.file.Path;

/** Layout: {@code datamarts/inverted_index.json} as {@code {"term": [bookId, ...]}} */
public class MonolithicJsonIndexAdapter implements InvertedIndexStorage {

    private final Path file;

    public MonolithicJsonIndexAdapter(Path file) {
        this.file = file;
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
