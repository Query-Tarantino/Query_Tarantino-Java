package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.Book;
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

/** Collection {@code tarantino.books}: one document per book, same fields as the SQLite table */
public class MongodbMetadataAdapter implements MetadataStorage {

    private final String connectionUri;

    public MongodbMetadataAdapter(String connectionUri) {
        this.connectionUri = connectionUri;
    }

    @Override
    public void save(Book book) {
        throw new UnsupportedOperationException("TODO");
    }
}
