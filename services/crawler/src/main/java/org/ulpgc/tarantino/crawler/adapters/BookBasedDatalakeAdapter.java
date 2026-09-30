package org.ulpgc.tarantino.crawler.adapters;

import org.ulpgc.tarantino.crawler.model.BookText;
import org.ulpgc.tarantino.crawler.model.StoredPaths;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.nio.file.Path;
import java.util.Optional;

public class BookBasedDatalakeAdapter implements DatalakeStorage {

    private static final String HEADER_FILE = "header.txt";
    private static final String BODY_FILE = "body.txt";

    private final Path root;

    public BookBasedDatalakeAdapter(Path root) {
        this.root = root;
    }

    @Override
    public StoredPaths save(BookText book) {
        return BookFiles.write(paths(book.bookId()), book);
    }

    @Override
    public Optional<StoredPaths> pathsOf(int bookId) {
        return BookFiles.existing(paths(bookId));
    }

    private StoredPaths paths(int bookId) {
        Path directory = root.resolve(String.valueOf(bookId));
        return new StoredPaths(directory.resolve(HEADER_FILE), directory.resolve(BODY_FILE));
    }
}
