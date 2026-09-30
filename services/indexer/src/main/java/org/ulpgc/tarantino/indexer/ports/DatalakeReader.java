package org.ulpgc.tarantino.indexer.ports;

import org.ulpgc.tarantino.indexer.model.BookText;

import java.util.Optional;

public interface DatalakeReader {

    Optional<BookText> read(int bookId);
}
