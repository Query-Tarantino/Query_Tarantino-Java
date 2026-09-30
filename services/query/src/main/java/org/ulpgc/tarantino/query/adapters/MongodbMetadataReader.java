package org.ulpgc.tarantino.query.adapters;

import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.util.List;
import java.util.Optional;

/** Reads {@code tarantino.books} */
public class MongodbMetadataReader implements MetadataReader {

    private final String connectionUri;

    public MongodbMetadataReader(String connectionUri) {
        this.connectionUri = connectionUri;
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
