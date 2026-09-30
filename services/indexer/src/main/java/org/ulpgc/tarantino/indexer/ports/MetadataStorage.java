package org.ulpgc.tarantino.indexer.ports;

import org.ulpgc.tarantino.indexer.model.Book;

public interface MetadataStorage {

    void save(Book book);
}
