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
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class MetadataBenchmark {

    @Param({"sqlite", "mongo"})
    public String metadata;

    @Param({"100", "1000", "10000"})
    public int books;

    private MetadataStorage storage;

    @Setup(Level.Trial)
    public void setUp() {
        Path datamarts = Path.of("benchmarks", "tmp", "metadata-" + metadata + "-" + books);
        storage = IndexerFactory.metadata(new IndexerConfig(Path.of("datalake"), "time", datamarts, "json",
                metadata, System.getenv().getOrDefault("TARANTINO_MONGO_URI", "mongodb://localhost:27017"),
                Path.of("workload")));
    }

    @Benchmark
    public void bulkInsertion() {
        throw new UnsupportedOperationException("Not implemented yet: save every parsed book");
    }
}
