package org.ulpgc.tarantino.query.benchmarking;

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
import org.ulpgc.tarantino.query.QueryConfig;
import org.ulpgc.tarantino.query.QueryFactory;
import org.ulpgc.tarantino.query.ports.InvertedIndexReader;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
public class QueryBenchmark {

    @Param({"json", "mongo", "folders"})
    public String index;

    @Param({"sqlite", "mongo"})
    public String metadata;

    private InvertedIndexReader invertedIndex;
    private MetadataReader metadataReader;

    @Setup(Level.Trial)
    public void setUp() {
        QueryConfig config = new QueryConfig(Path.of("datamarts"), index, metadata,
                System.getenv().getOrDefault("TARANTINO_MONGO_URI", "mongodb://localhost:27017"), Path.of("workload"));
        invertedIndex = QueryFactory.invertedIndex(config);
        metadataReader = QueryFactory.metadata(config);
    }

    @Benchmark
    public void workloadSearch() {
        throw new UnsupportedOperationException("Not implemented yet: run every query of the workload against the inverted index");
    }

    @Benchmark
    public void booksByAuthor() {
        throw new UnsupportedOperationException("Not implemented yet: metadataReader.booksBy for a fixed author");
    }

    @Benchmark
    public void bookById() {
        throw new UnsupportedOperationException("Not implemented yet: metadataReader.book for a random book id");
    }
}
