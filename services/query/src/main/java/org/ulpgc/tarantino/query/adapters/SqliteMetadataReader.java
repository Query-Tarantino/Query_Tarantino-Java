package org.ulpgc.tarantino.query.adapters;

import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Reads {@code datamarts/metadata.db} */
public class SqliteMetadataReader implements MetadataReader {

    private final Path database;

    public SqliteMetadataReader(Path database) {
        this.database = database;
    }

    @Override
    public Optional<BookMetadata> findById(int bookId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<BookMetadata> findByAuthor(String author) {
        throw new UnsupportedOperationException("TODO");
    }
}
