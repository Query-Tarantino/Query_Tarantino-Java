package org.ulpgc.tarantino.query.ports;

import org.ulpgc.tarantino.query.model.BookMetadata;

import java.util.List;
import java.util.Optional;

public interface MetadataReader {

    Optional<BookMetadata> findById(int bookId);

    List<BookMetadata> findByAuthor(String author);
}
