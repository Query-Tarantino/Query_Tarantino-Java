package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.Book;
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

import java.nio.file.Path;

/** Table {@code books(book_id, title, author, language, path)} in {@code datamarts/metadata.db} */
public class SqliteMetadataAdapter implements MetadataStorage {

    private final Path database;

    public SqliteMetadataAdapter(Path database) {
        this.database = database;
    }

    @Override
    public void save(Book book) {
        throw new UnsupportedOperationException("TODO");
    }
}
