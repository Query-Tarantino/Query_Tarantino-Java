package org.ulpgc.tarantino.control.adapters;

import org.ulpgc.tarantino.control.ports.ControlStateStore;

import java.nio.file.Path;
import java.util.Set;

/** Layout: {@code control/downloaded_books.txt} and {@code control/indexed_books.txt}, one bookId per line */
public class FileControlStateStore implements ControlStateStore {

    private final Path root;

    public FileControlStateStore(Path root) {
        this.root = root;
    }

    @Override
    public Set<Integer> downloaded() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Set<Integer> indexed() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void markDownloaded(int bookId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void markIndexed(int bookId) {
        throw new UnsupportedOperationException("TODO");
    }
}
