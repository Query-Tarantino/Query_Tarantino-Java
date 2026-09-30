package org.ulpgc.tarantino.crawler;

import org.ulpgc.tarantino.crawler.adapters.BatchBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.adapters.BookBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.adapters.GutenbergHttpDownloader;
import org.ulpgc.tarantino.crawler.adapters.TimeBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class CrawlerFactory {

    private static final Map<String, Function<Path, DatalakeStorage>> DATALAKE_LAYOUTS = Map.of(
            "time", TimeBasedDatalakeAdapter::new,
            "book", BookBasedDatalakeAdapter::new,
            "batch", BatchBasedDatalakeAdapter::new);

    private CrawlerFactory() {
    }

    public static IngestBookCommand ingestCommand(CrawlerConfig config) {
        return new IngestBookCommand(new GutenbergHttpDownloader(), datalake(config));
    }

    public static DatalakeStorage datalake(CrawlerConfig config) {
        return option(DATALAKE_LAYOUTS, config.datalakeLayout(), "datalake layout").apply(config.datalake());
    }

    private static <T> T option(Map<String, T> options, String name, String kind) {
        return Optional.ofNullable(options.get(name))
                .orElseThrow(() -> new IllegalArgumentException("Unknown " + kind + ": " + name));
    }
}
