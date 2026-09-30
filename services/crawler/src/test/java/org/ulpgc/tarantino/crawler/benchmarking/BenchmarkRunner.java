package org.ulpgc.tarantino.crawler.benchmarking;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.ulpgc.tarantino.crawler.benchmarking.support.BenchmarkDataset;
import org.ulpgc.tarantino.crawler.benchmarking.support.BenchmarkOptions;
import org.ulpgc.tarantino.crawler.benchmarking.support.BenchmarkPaths;
import org.ulpgc.tarantino.crawler.benchmarking.support.DatalakeFixture;
import org.ulpgc.tarantino.crawler.benchmarking.support.Directories;
import org.ulpgc.tarantino.crawler.benchmarking.support.FootprintLog;
import org.ulpgc.tarantino.crawler.benchmarking.support.JmhResults;
import org.ulpgc.tarantino.crawler.benchmarking.support.Metric;
import org.ulpgc.tarantino.crawler.benchmarking.support.RecoveryScenario;
import org.ulpgc.tarantino.crawler.benchmarking.support.ResultRow;
import org.ulpgc.tarantino.crawler.benchmarking.support.ResultsFile;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class BenchmarkRunner {

    static final String SERVICE = "crawler";

    private static final int RECOVERY_BOOKS = 100;
    private static final Map<String, Metric> METRICS = Map.of(
            "writeThroughput", Metric.booksPerSecond("write_throughput"),
            "lookupTime", Metric.asMeasured("lookup_time", "µs/op"),
            "newBooksDetectionTime", Metric.asMeasured("new_books_detection_time", "ms"));

    public static void main(String[] args) throws RunnerException {
        FootprintLog.drain(SERVICE);
        Collection<RunResult> results = new Runner(BenchmarkOptions.forService(SERVICE).build()).run();
        System.out.println("Results written to " + ResultsFile.write(SERVICE, rows(results)));
    }

    private static List<ResultRow> rows(Collection<RunResult> results) {
        return Stream.of(JmhResults.rows(results, METRICS), FootprintLog.drain(SERVICE), recoveryRows())
                .flatMap(List::stream)
                .toList();
    }

    private static List<ResultRow> recoveryRows() {
        BenchmarkDataset dataset = BenchmarkDataset.fromEnvironment();
        return DatalakeFixture.LAYOUTS.stream().map(layout -> recoveryRow(layout, dataset)).toList();
    }

    private static ResultRow recoveryRow(String layout, BenchmarkDataset dataset) {
        Path root = BenchmarkPaths.scratch("datalake-recovery-" + layout);
        Directories.delete(root);
        boolean recovered = new RecoveryScenario(layout, root, dataset::rawText).succeedsFor(dataset.ids(RECOVERY_BOOKS));
        Directories.delete(root);
        return new ResultRow(layout, "recovery_ok", RECOVERY_BOOKS, recovered ? 1 : 0, "0 or 1");
    }
}
