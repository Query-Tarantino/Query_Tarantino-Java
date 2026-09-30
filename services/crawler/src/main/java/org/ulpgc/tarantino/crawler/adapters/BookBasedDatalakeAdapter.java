package org.ulpgc.tarantino.crawler.adapters;

import org.ulpgc.tarantino.crawler.model.BookText;
import org.ulpgc.tarantino.crawler.model.StoredPaths;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.nio.file.Path;
import java.util.Optional;

/** Layout: {@code datalake/<id>/header.txt + body.txt} */
public class BookBasedDatalakeAdapter implements DatalakeStorage {

    private final Path root;

    public BookBasedDatalakeAdapter(Path root) {
        this.root = root;
    }

    @Override
    public StoredPaths save(BookText book) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Optional<StoredPaths> locate(int bookId) {
        throw new UnsupportedOperationException("TODO");
    }
}
