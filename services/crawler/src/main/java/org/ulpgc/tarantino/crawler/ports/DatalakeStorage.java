package org.ulpgc.tarantino.crawler.ports;

import org.ulpgc.tarantino.crawler.model.book.BookText;
import org.ulpgc.tarantino.crawler.model.book.StoredPaths;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;

public interface DatalakeStorage {

    StoredPaths save(BookText book);

    Optional<StoredPaths> pathsOf(int bookId);

    Set<Integer> idsStoredSince(Instant instant);
}
