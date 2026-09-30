package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

import java.nio.file.Path;

public class FolderPerTermIndexAdapter implements InvertedIndexStorage {

    private final Path root;

    public FolderPerTermIndexAdapter(Path root) {
        this.root = root;
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
