package org.ulpgc.tarantino.control.adapters;

import org.ulpgc.tarantino.control.model.Outcome;
import org.ulpgc.tarantino.control.ports.Crawler;
import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;
import org.ulpgc.tarantino.crawler.commands.IngestResult;

public class LocalCrawler implements Crawler {

    private final IngestBookCommand ingest;

    public LocalCrawler(IngestBookCommand ingest) {
        this.ingest = ingest;
    }

    @Override
    public Outcome ingest(int bookId) {
        return outcome(ingest.execute(bookId));
    }

    private static Outcome outcome(IngestResult result) {
        return result.succeeded()
                ? Outcome.success("stored in " + result.paths().body().getParent())
                : Outcome.failure("skipped, " + result.failure());
    }
}
