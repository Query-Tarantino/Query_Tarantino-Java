package org.ulpgc.tarantino.query.adapters;

import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.util.List;
import java.util.Optional;

public class MongodbMetadataReader implements MetadataReader {

    private final String connectionUri;

    public MongodbMetadataReader(String connectionUri) {
        this.connectionUri = connectionUri;
    }

    @Override
    public Optional<BookMetadata> book(int bookId) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<BookMetadata> booksBy(String author) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
