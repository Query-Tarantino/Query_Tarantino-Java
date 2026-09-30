package org.ulpgc.tarantino.crawler.commands;

import org.ulpgc.tarantino.crawler.model.BookText;
import org.ulpgc.tarantino.crawler.model.DownloadException;
import org.ulpgc.tarantino.crawler.model.FailureReason;
import org.ulpgc.tarantino.crawler.model.GutenbergText;
import org.ulpgc.tarantino.crawler.model.StoredPaths;
import org.ulpgc.tarantino.crawler.ports.BookDownloader;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.io.UncheckedIOException;
import java.util.Optional;

public class IngestBookCommand {

    private final BookDownloader downloader;
    private final DatalakeStorage datalake;

    public IngestBookCommand(BookDownloader downloader, DatalakeStorage datalake) {
        this.downloader = downloader;
        this.datalake = datalake;
    }

    /** Idempotent: a book already in the datalake is not downloaded again, so resuming never duplicates it. */
    public IngestResult execute(int bookId) {
        Optional<StoredPaths> existing = datalake.locate(bookId);
        if (existing.isPresent()) {
            return IngestResult.success(bookId, existing.get());
        }
        try {
            BookText book = GutenbergText.split(bookId, downloader.download(bookId));
            return IngestResult.success(bookId, datalake.save(book));
        } catch (DownloadException e) {
            return IngestResult.failure(bookId, e.reason());
        } catch (UncheckedIOException e) {
            return IngestResult.failure(bookId, FailureReason.STORAGE_ERROR);
        }
    }
}
