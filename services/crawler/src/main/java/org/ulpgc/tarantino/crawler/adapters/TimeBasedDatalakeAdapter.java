package org.ulpgc.tarantino.crawler.adapters;

import org.ulpgc.tarantino.crawler.model.BookText;
import org.ulpgc.tarantino.crawler.model.StoredPaths;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.nio.file.Path;
import java.util.Optional;

/** Layout: {@code datalake/YYYYMMDD/HH/<id>.header.txt + <id>.body.txt} */
public class TimeBasedDatalakeAdapter implements DatalakeStorage {

    private final Path root;

    public TimeBasedDatalakeAdapter(Path root) {
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
