package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.BookText;
import org.ulpgc.tarantino.indexer.ports.DatalakeReader;

import java.nio.file.Path;
import java.util.Optional;

public class BatchBasedDatalakeReader implements DatalakeReader {

    private final Path root;

    public BatchBasedDatalakeReader(Path root) {
        this.root = root;
    }

    @Override
    public Optional<BookText> bookText(int bookId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
