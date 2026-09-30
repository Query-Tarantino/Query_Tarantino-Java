package org.ulpgc.tarantino.indexer.benchmarking;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(1)
public class IndexingBenchmark {

    @Param({"json", "mongo", "folders"})
    public String index;

    @Param({"100", "1000", "10000"})
    public int books;

    @Benchmark
    public void fullIndexBuild() {
        throw new UnsupportedOperationException("Not implemented yet: add every book to an empty index and flush");
    }

    @Benchmark
    public void incrementalUpdate() {
        throw new UnsupportedOperationException("Not implemented yet: add a fixed batch of new books to an index that already holds `books`");
    }
}
