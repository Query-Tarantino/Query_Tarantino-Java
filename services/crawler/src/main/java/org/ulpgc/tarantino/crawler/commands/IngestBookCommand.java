package org.ulpgc.tarantino.crawler.commands;

import org.ulpgc.tarantino.crawler.model.book.GutenbergText;
import org.ulpgc.tarantino.crawler.model.failure.DownloadException;
import org.ulpgc.tarantino.crawler.model.failure.FailureReason;
import org.ulpgc.tarantino.crawler.ports.BookDownloader;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.io.UncheckedIOException;

public class IngestBookCommand {

    private final BookDownloader downloader;
    private final DatalakeStorage datalake;

    public IngestBookCommand(BookDownloader downloader, DatalakeStorage datalake) {
        this.downloader = downloader;
        this.datalake = datalake;
    }

    public IngestResult execute(int bookId) {
        return datalake.pathsOf(bookId)
                .map(paths -> IngestResult.success(bookId, paths))
                .orElseGet(() -> downloadAndStore(bookId));
    }

    private IngestResult downloadAndStore(int bookId) {
        try {
            return IngestResult.success(bookId, datalake.save(GutenbergText.bookText(bookId, downloader.rawText(bookId))));
        } catch (DownloadException e) {
            return IngestResult.failure(bookId, e.reason());
        } catch (UncheckedIOException e) {
            return IngestResult.failure(bookId, FailureReason.STORAGE_ERROR);
        }
    }
}
