package org.ulpgc.tarantino.query.adapters;

import org.ulpgc.tarantino.query.ports.InvertedIndexReader;

import java.nio.file.Path;
import java.util.Set;

/** Reads {@code datamarts/inverted_index/<FIRST_LETTER>/<term>.txt} */
public class FolderPerTermIndexReader implements InvertedIndexReader {

    private final Path root;

    public FolderPerTermIndexReader(Path root) {
        this.root = root;
    }

    @Override
    public Set<Integer> postings(String term) {
        throw new UnsupportedOperationException("TODO");
    }
}
