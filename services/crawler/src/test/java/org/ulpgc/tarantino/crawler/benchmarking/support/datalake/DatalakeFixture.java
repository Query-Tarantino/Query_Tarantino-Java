package org.ulpgc.tarantino.crawler.benchmarking.support.datalake;

import org.ulpgc.tarantino.crawler.CrawlerConfig;
import org.ulpgc.tarantino.crawler.CrawlerFactory;
import org.ulpgc.tarantino.crawler.adapters.datalake.time.TimeBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.benchmarking.support.dataset.BenchmarkDataset;
import org.ulpgc.tarantino.crawler.benchmarking.support.files.Directories;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.ResultRow;
import org.ulpgc.tarantino.crawler.model.book.GutenbergText;
import org.ulpgc.tarantino.crawler.model.book.StoredPaths;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.time.Clock;
import java.util.List;

public final class DatalakeFixture {

    public static final List<String> LAYOUTS = List.of("time", "book", "batch");
    public static final Instant CRAWL_START = Instant.parse("2025-09-25T00:00:00Z");

    private DatalakeFixture() {
    }

    public static DatalakeStorage datalake(String layout, Path root) {
        return datalake(layout, root, Clock.systemUTC());
    }

    public static DatalakeStorage datalake(String layout, Path root, Clock clock) {
        return "time".equals(layout) ? new TimeBasedDatalakeAdapter(root, clock) : CrawlerFactory.datalake(new CrawlerConfig(root, layout));
    }

    public static List<StoredPaths> store(DatalakeStorage datalake, List<Integer> ids, BenchmarkDataset dataset) {
        return ids.stream().map(id -> save(datalake, id, dataset)).toList();
    }

    public static List<StoredPaths> storeAsCrawled(String layout, Path root, List<Integer> ids, BenchmarkDataset dataset, Instant start) {
        CrawlClock clock = new CrawlClock(start);
        DatalakeStorage datalake = datalake(layout, root, clock);
        List<StoredPaths> stored = new ArrayList<>(ids.size());
        for (int position = 0; position < ids.size(); position++) {
            clock.moveTo(position);
            stored.add(save(datalake, ids.get(position), dataset));
        }
        return stored;
    }

    private static StoredPaths save(DatalakeStorage datalake, int bookId, BenchmarkDataset dataset) {
        return datalake.save(GutenbergText.bookText(bookId, dataset.rawText(bookId)));
    }

    public static List<ResultRow> footprint(String layout, int books, Path root) {
        return List.of(
                ResultRow.exact(layout, "file_count", books, Directories.fileCount(root), "files"),
                ResultRow.exact(layout, "directory_count", books, Directories.directoryCount(root), "dirs"),
                ResultRow.exact(layout, "disk_usage", books, Directories.diskUsage(root), "bytes"));
    }
}
