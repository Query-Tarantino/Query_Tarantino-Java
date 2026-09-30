package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

import java.nio.file.Path;

/** Layout: {@code datamarts/inverted_index/<FIRST_LETTER>/<term>.txt}, one bookId per line */
public class FolderPerTermIndexAdapter implements InvertedIndexStorage {

    private final Path root;

    public FolderPerTermIndexAdapter(Path root) {
        this.root = root;
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
