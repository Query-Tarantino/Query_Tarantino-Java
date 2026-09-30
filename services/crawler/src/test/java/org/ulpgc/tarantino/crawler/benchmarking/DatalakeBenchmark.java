package org.ulpgc.tarantino.crawler.benchmarking;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.ulpgc.tarantino.crawler.CrawlerConfig;
import org.ulpgc.tarantino.crawler.CrawlerFactory;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Datalake structures (PDF 3.1). Reads raw texts from {@code benchmarks/cache/} so the
 * network is never measured. Storage overhead (files, directories) is counted, not timed.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class DatalakeBenchmark {

    @Param({"time", "book", "batch"})
    public String layout;

    @Param({"100", "1000", "10000"})
    public int books;

    private DatalakeStorage datalake;

    @Setup(Level.Trial)
    public void setUp() {
        // TODO: load the first `books` ids of workload/book_ids.txt from benchmarks/cache/
        Path root = Path.of("benchmarks", "tmp", "datalake-" + layout + "-" + books);
        datalake = CrawlerFactory.datalake(new CrawlerConfig(root, layout));
    }

    @Benchmark
    public void writeThroughput() {
        // TODO: split and save every cached book
    }

    @Benchmark
    public void lookup() {
        // TODO: locate header and body of a random stored book
    }

    @Benchmark
    public void detectNewBooks() {
        // TODO: find books stored after a given point in time
    }
}
