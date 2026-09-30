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
public class MetadataBenchmark {

    @Param({"sqlite", "mongo"})
    public String metadata;

    @Param({"100", "1000", "10000"})
    public int books;

    @Benchmark
    public void bulkInsertion() {
        throw new UnsupportedOperationException("Not implemented yet: save every parsed book");
    }
}
