package org.ulpgc.tarantino.indexer.benchmarking;

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
import org.ulpgc.tarantino.indexer.IndexerConfig;
import org.ulpgc.tarantino.indexer.IndexerFactory;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Inverted index structures (PDF 4.2). Memory comes from the GC profiler; disk usage is measured after the run. */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class IndexingBenchmark {

    @Param({"json", "mongo", "folders"})
    public String index;

    @Param({"100", "1000", "10000"})
    public int books;

    private InvertedIndexStorage invertedIndex;

    @Setup(Level.Trial)
    public void setUp() {
        // TODO: pre-tokenize the first `books` ids of workload/book_ids.txt
        Path datamarts = Path.of("benchmarks", "tmp", "datamarts-" + index + "-" + books);
        invertedIndex = IndexerFactory.invertedIndex(config(datamarts));
    }

    @Benchmark
    public void buildIndex() {
        // TODO: add every book to an empty index and flush
    }

    @Benchmark
    public void addBooksToExistingIndex() {
        // TODO: add a fixed batch of new books to an index that already holds `books`
    }

    private IndexerConfig config(Path datamarts) {
        return new IndexerConfig(Path.of("datalake"), "time", datamarts, index, "sqlite",
                System.getenv().getOrDefault("TARANTINO_MONGO_URI", "mongodb://localhost:27017"),
                Path.of("workload"));
    }
}
