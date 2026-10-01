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
import org.ulpgc.tarantino.crawler.benchmarking.support.environment.Heap;
import org.ulpgc.tarantino.crawler.benchmarking.support.files.FootprintLog;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.ResultRow;
import org.ulpgc.tarantino.indexer.benchmarking.BenchmarkRunner;
import org.ulpgc.tarantino.indexer.benchmarking.support.BenchmarkStore;
import org.ulpgc.tarantino.indexer.benchmarking.support.IndexFixture;
import org.ulpgc.tarantino.indexer.ports.datamarts.InvertedIndexStorage;

import java.lang.ref.Reference;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@Fork(value = 3, jvmArgsAppend = {"-Xmx4g", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"})
public class FullIndexBuildBenchmark {

    @Param({"json", "folders", "mongo"})
    public String index;

    @Param({"100", "500", "1000", "2000"})
    public int books;

    private IndexFixture fixture;
    private BenchmarkStore store;
    private List<Integer> ids;

    @Setup(Level.Trial)
    public void selectBooks() {
        fixture = IndexFixture.fromEnvironment();
        store = BenchmarkStore.forIndex(index, "index-build-" + index + "-" + books);
        ids = fixture.dataset().ids(books);
    }

    @Setup(Level.Iteration)
    public void emptyIndex() {
        store.clear();
    }

    @Benchmark
    public void fullBuildTime() {
        fixture.index(store.invertedIndex(), ids);
    }

    @TearDown(Level.Trial)
    public void recordFootprint() {
        FootprintLog.append(BenchmarkRunner.SERVICE, List.of(
                ResultRow.exact(index, "disk_usage", books, store.diskUsage(), "bytes"),
                ResultRow.exact(index, "term_count", books, store.termCount(), "terms")));
        store.clear();
        FootprintLog.appendSample(BenchmarkRunner.SERVICE, ResultRow.sample(index, "build_memory", books, buildMemory(), "bytes"));
        store.clear();
    }

    private long buildMemory() {
        long before = Heap.usedAfterFullCollection();
        InvertedIndexStorage invertedIndex = store.invertedIndex();
        fixture.add(invertedIndex, ids);
        long retained = Heap.usedAfterFullCollection() - before;
        Reference.reachabilityFence(invertedIndex);
        return retained;
    }
}
