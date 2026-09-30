package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.Book;
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

public class MongodbMetadataAdapter implements MetadataStorage {

    private final String connectionUri;

    public MongodbMetadataAdapter(String connectionUri) {
        this.connectionUri = connectionUri;
    }

    @Override
    public void save(Book book) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
