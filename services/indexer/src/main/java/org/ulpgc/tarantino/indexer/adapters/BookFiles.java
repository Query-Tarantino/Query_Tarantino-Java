package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.BookText;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

final class BookFiles {

    private BookFiles() {
    }

    static Optional<BookText> bookText(int bookId, Path header, Path body) {
        return Files.exists(body) ? Optional.of(new BookText(bookId, content(header), content(body), body)) : Optional.empty();
    }

    private static String content(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
