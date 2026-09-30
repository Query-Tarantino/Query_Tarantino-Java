package org.ulpgc.tarantino.crawler.benchmarking;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.ulpgc.tarantino.crawler.benchmarking.support.BenchmarkDataset;
import org.ulpgc.tarantino.crawler.benchmarking.support.BenchmarkPaths;
import org.ulpgc.tarantino.crawler.benchmarking.support.DatalakeFixture;
import org.ulpgc.tarantino.crawler.benchmarking.support.Directories;
import org.ulpgc.tarantino.crawler.model.StoredPaths;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsAppend = {"-Xmx4g", "--sun-misc-unsafe-memory-access=allow"})
public class NewBooksDetectionBenchmark {

    private static final int NEW_BOOKS = 100;
    private static final Duration AGE_OF_OLD_BOOKS = Duration.ofDays(1);

    @Param({"time", "book", "batch"})
    public String layout;

    @Param({"100", "500", "1000", "2000"})
    public int books;

    private Path root;
    private DatalakeStorage datalake;
    private Instant lastRun;

    @Setup(Level.Trial)
    public void storeOldAndNewBooks() {
        BenchmarkDataset dataset = BenchmarkDataset.fromEnvironment();
        root = BenchmarkPaths.scratch("datalake-detection-" + layout + "-" + books);
        Directories.delete(root);
        lastRun = Instant.now();
        storeOldBooks(dataset.ids(books), dataset);
        datalake = DatalakeFixture.datalake(layout, root);
        DatalakeFixture.store(datalake, dataset.ids(books, NEW_BOOKS), dataset);
    }

    @Benchmark
    public Set<Integer> newBooksDetectionTime() {
        return datalake.idsStoredSince(lastRun);
    }

    @TearDown(Level.Trial)
    public void deleteDatalake() {
        Directories.delete(root);
    }

    private void storeOldBooks(List<Integer> ids, BenchmarkDataset dataset) {
        Instant oldRun = lastRun.minus(AGE_OF_OLD_BOOKS);
        DatalakeStorage oldDatalake = DatalakeFixture.datalake(layout, root, Clock.fixed(oldRun, ZoneOffset.UTC));
        DatalakeFixture.store(oldDatalake, ids, dataset).forEach(paths -> age(paths, oldRun));
    }

    private static void age(StoredPaths paths, Instant time) {
        try {
            Files.setLastModifiedTime(paths.body(), FileTime.from(time));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
