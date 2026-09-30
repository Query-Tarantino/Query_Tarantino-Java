package org.ulpgc.tarantino.control.adapters;

import org.ulpgc.tarantino.control.ports.ControlStateStore;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Layout: {@code control/downloaded_books.txt} and {@code control/indexed_books.txt}, one bookId per line */
public class FileControlStateStore implements ControlStateStore {

    private final Path downloaded;
    private final Path indexed;

    public FileControlStateStore(Path root) {
        this.downloaded = root.resolve("downloaded_books.txt");
        this.indexed = root.resolve("indexed_books.txt");
    }

    @Override
    public Set<Integer> downloaded() {
        return read(downloaded);
    }

    @Override
    public Set<Integer> indexed() {
        return read(indexed);
    }

    @Override
    public void markDownloaded(int bookId) {
        append(downloaded, bookId);
    }

    @Override
    public void markIndexed(int bookId) {
        append(indexed, bookId);
    }

    /** Keeps file order; blank lines and a partially written last line are ignored. */
    private static Set<Integer> read(Path file) {
        if (!Files.exists(file)) {
            return new LinkedHashSet<>();
        }
        try {
            return Files.readAllLines(file).stream()
                    .map(String::strip)
                    .filter(line -> line.matches("\\d+"))
                    .map(Integer::valueOf)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void append(Path file, int bookId) {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Files.writeString(file, bookId + System.lineSeparator(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
