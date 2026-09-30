package org.ulpgc.tarantino.indexer.benchmarking;

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
import org.ulpgc.tarantino.indexer.benchmarking.support.BenchmarkStore;
import org.ulpgc.tarantino.indexer.benchmarking.support.IndexFixture;

import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(value = 1, jvmArgsAppend = {"-Xmx4g", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"})
public class IncrementalUpdateBenchmark {

    private static final int NEW_BOOKS = 100;

    @Param({"json", "folders", "mongo"})
    public String index;

    @Param({"100", "500", "1000", "2000"})
    public int books;

    private IndexFixture fixture;
    private BenchmarkStore store;
    private int updates;
    private List<Integer> newIds;

    @Setup(Level.Trial)
    public void buildIndex() {
        fixture = IndexFixture.fromEnvironment();
        store = BenchmarkStore.forIndex(index, "index-update-" + index + "-" + books);
        store.clear();
        fixture.index(store.invertedIndex(), fixture.dataset().ids(books));
    }

    @Setup(Level.Iteration)
    public void selectNewBooks() {
        newIds = fixture.dataset().ids(books + NEW_BOOKS * updates++, NEW_BOOKS);
    }

    @Benchmark
    public void incrementalUpdateTime() {
        fixture.index(store.invertedIndex(), newIds);
    }

    @TearDown(Level.Trial)
    public void deleteIndex() {
        store.clear();
    }
}
