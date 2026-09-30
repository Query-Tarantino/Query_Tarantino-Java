package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.BookText;
import org.ulpgc.tarantino.indexer.ports.DatalakeReader;

import java.nio.file.Path;
import java.util.Optional;

/** Layout: {@code datalake/YYYYMMDD/HH/<id>.header.txt + <id>.body.txt} */
public class TimeBasedDatalakeReader implements DatalakeReader {

    private final Path root;

    public TimeBasedDatalakeReader(Path root) {
        this.root = root;
    }

    @Override
    public Optional<BookText> read(int bookId) {
        throw new UnsupportedOperationException("TODO");
    }
}
