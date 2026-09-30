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
import java.time.Clock;
import java.util.List;

public final class DatalakeFixture {

    public static final List<String> LAYOUTS = List.of("time", "book", "batch");

    private DatalakeFixture() {
    }

    public static DatalakeStorage datalake(String layout, Path root) {
        return datalake(layout, root, Clock.systemUTC());
    }

    public static DatalakeStorage datalake(String layout, Path root, Clock clock) {
        return "time".equals(layout) ? new TimeBasedDatalakeAdapter(root, clock) : CrawlerFactory.datalake(new CrawlerConfig(root, layout));
    }

    public static List<StoredPaths> store(DatalakeStorage datalake, List<Integer> ids, BenchmarkDataset dataset) {
        return ids.stream().map(id -> datalake.save(GutenbergText.bookText(id, dataset.rawText(id)))).toList();
    }

    public static List<ResultRow> footprint(String layout, int books, Path root) {
        return List.of(
                new ResultRow(layout, "file_count", books, Directories.fileCount(root), "files"),
                new ResultRow(layout, "directory_count", books, Directories.directoryCount(root), "dirs"),
                new ResultRow(layout, "disk_usage", books, Directories.diskUsage(root), "bytes"));
    }
}
