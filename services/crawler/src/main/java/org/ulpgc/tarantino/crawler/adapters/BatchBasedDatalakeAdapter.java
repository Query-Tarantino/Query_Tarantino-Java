package org.ulpgc.tarantino.crawler.adapters;

import org.ulpgc.tarantino.crawler.model.BookText;
import org.ulpgc.tarantino.crawler.model.StoredPaths;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.nio.file.Path;
import java.util.Optional;

public class BatchBasedDatalakeAdapter implements DatalakeStorage {

    private final Path root;

    public BatchBasedDatalakeAdapter(Path root) {
        this.root = root;
    }

    @Override
    public StoredPaths save(BookText book) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Optional<StoredPaths> pathsOf(int bookId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
