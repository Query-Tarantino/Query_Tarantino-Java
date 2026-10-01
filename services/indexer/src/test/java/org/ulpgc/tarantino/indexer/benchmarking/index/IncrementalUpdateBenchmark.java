package org.ulpgc.tarantino.indexer.benchmarking.index;

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
@Warmup(iterations = 2)
@Measurement(iterations = 3)
@Fork(value = 3, jvmArgsAppend = {"-Xmx4g", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"})
public class IncrementalUpdateBenchmark {

    @Param({"json", "folders", "mongo"})
    public String index;

    @Param({"100", "500", "1000", "2000"})
    public int books;

    private IndexFixture fixture;
    private BenchmarkStore snapshot;
    private BenchmarkStore store;
    private List<Integer> newIds;

    @Setup(Level.Trial)
    public void buildSnapshot() {
        fixture = IndexFixture.fromEnvironment();
        snapshot = BenchmarkStore.forIndex(index, "index-update-snapshot-" + index + "-" + books);
        store = BenchmarkStore.forIndex(index, "index-update-" + index + "-" + books);
        snapshot.clear();
        fixture.index(snapshot.invertedIndex(), fixture.dataset().ids(books));
        newIds = fixture.dataset().newIds();
    }

    @Setup(Level.Iteration)
    public void restoreIndex() {
        snapshot.copyTo(store);
    }

    @Benchmark
    public void incrementalUpdateTime() {
        fixture.index(store.invertedIndex(), newIds);
    }

    @TearDown(Level.Trial)
    public void deleteIndexes() {
        store.clear();
        snapshot.clear();
    }
}
