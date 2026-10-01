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
import org.ulpgc.tarantino.crawler.benchmarking.support.dataset.BenchmarkDataset;
import org.ulpgc.tarantino.indexer.benchmarking.support.BenchmarkStore;
import org.ulpgc.tarantino.indexer.benchmarking.support.IndexFixture;
import org.ulpgc.tarantino.indexer.benchmarking.support.PrebuiltIndexes;
import org.ulpgc.tarantino.indexer.ports.datamarts.InvertedIndexStorage;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 1)
@Measurement(iterations = 3)
@Fork(value = 3, jvmArgsAppend = {"-Xmx4g", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"})
public class IncrementalUpdateBenchmark {

    // Flushing after every book is far slower (folders rewrites every term file of each book), so fewer books
    public static final int BOOKS_FLUSHED_ONE_BY_ONE = 10;
    public static final int BOOKS_FLUSHED_TOGETHER = BenchmarkDataset.NEW_BOOKS;

    @Param({"json", "folders", "mongo"})
    public String index;

    @Param({"100", "500", "1000", "2000"})
    public int books;

    private IndexFixture fixture;
    private BenchmarkStore snapshot;
    private BenchmarkStore store;
    private List<Integer> newIds;
    private List<Integer> booksFlushedOneByOne;
    private Set<String> touchedTerms;
    private boolean copied;

    @Setup(Level.Trial)
    public void selectSnapshot() {
        fixture = IndexFixture.fromEnvironment();
        snapshot = PrebuiltIndexes.of(index, books, fixture);
        store = BenchmarkStore.forIndex(index, "index-update-" + index + "-" + books);
        newIds = fixture.dataset().newIds();
        booksFlushedOneByOne = newIds.subList(0, BOOKS_FLUSHED_ONE_BY_ONE);
        touchedTerms = newIds.stream().flatMap(bookId -> fixture.terms(bookId).stream()).collect(Collectors.toSet());
    }

    @Setup(Level.Iteration)
    public void restoreIndex() {
        if (copied) {
            store.restoreTermsFrom(snapshot, touchedTerms);
        } else {
            snapshot.copyTo(store);
            copied = true;
        }
    }

    /** As the control layer indexes (SPEC §9): one open index, flushed after every book. */
    @Benchmark
    public void incrementalUpdateTime() {
        InvertedIndexStorage invertedIndex = store.invertedIndex();
        booksFlushedOneByOne.forEach(bookId -> fixture.index(invertedIndex, List.of(bookId)));
    }

    @Benchmark
    public void batchUpdateTime() {
        fixture.index(store.invertedIndex(), newIds);
    }

    @TearDown(Level.Trial)
    public void deleteIndex() {
        store.clear();
    }
}
