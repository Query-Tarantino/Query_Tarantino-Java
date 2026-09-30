package org.ulpgc.tarantino.crawler;

import org.ulpgc.tarantino.crawler.adapters.BatchBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.adapters.BookBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.adapters.GutenbergHttpDownloader;
import org.ulpgc.tarantino.crawler.adapters.TimeBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

public final class CrawlerFactory {

    private CrawlerFactory() {
    }

    public static IngestBookCommand ingestCommand(CrawlerConfig config) {
        return new IngestBookCommand(new GutenbergHttpDownloader(), datalake(config));
    }

    public static DatalakeStorage datalake(CrawlerConfig config) {
        return switch (config.datalakeLayout()) {
            case "time" -> new TimeBasedDatalakeAdapter(config.datalake());
            case "book" -> new BookBasedDatalakeAdapter(config.datalake());
            case "batch" -> new BatchBasedDatalakeAdapter(config.datalake());
            default -> throw new IllegalArgumentException("Unknown datalake layout: " + config.datalakeLayout());
        };
    }
}
