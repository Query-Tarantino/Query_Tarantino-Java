package org.ulpgc.tarantino.crawler.commands;

import org.ulpgc.tarantino.crawler.ports.BookDownloader;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

public class IngestBookCommand {

    private final BookDownloader downloader;
    private final DatalakeStorage datalake;

    public IngestBookCommand(BookDownloader downloader, DatalakeStorage datalake) {
        this.downloader = downloader;
        this.datalake = datalake;
    }

    public IngestResult execute(int bookId) {
        // TODO: download -> GutenbergText.split -> datalake.save
        throw new UnsupportedOperationException("TODO");
    }
}
