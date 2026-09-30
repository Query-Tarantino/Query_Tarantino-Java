package org.ulpgc.tarantino.crawler.commands;

import org.ulpgc.tarantino.crawler.model.FailureReason;
import org.ulpgc.tarantino.crawler.model.StoredPaths;

public record IngestResult(int bookId, StoredPaths paths, FailureReason failure) {

    public static IngestResult success(int bookId, StoredPaths paths) {
        return new IngestResult(bookId, paths, null);
    }

    public static IngestResult failure(int bookId, FailureReason reason) {
        return new IngestResult(bookId, null, reason);
    }

    public boolean succeeded() {
        return failure == null;
    }
}
