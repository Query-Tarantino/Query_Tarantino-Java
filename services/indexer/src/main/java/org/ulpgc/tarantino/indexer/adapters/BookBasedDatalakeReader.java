package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.BookText;
import org.ulpgc.tarantino.indexer.ports.DatalakeReader;

import java.nio.file.Path;
import java.util.Optional;

public class BookBasedDatalakeReader implements DatalakeReader {

    private static final String HEADER_FILE = "header.txt";
    private static final String BODY_FILE = "body.txt";

    private final Path root;

    public BookBasedDatalakeReader(Path root) {
        this.root = root;
    }

    @Override
    public Optional<BookText> bookText(int bookId) {
        Path directory = root.resolve(String.valueOf(bookId));
        return BookFiles.bookText(bookId, directory.resolve(HEADER_FILE), directory.resolve(BODY_FILE));
    }
}
