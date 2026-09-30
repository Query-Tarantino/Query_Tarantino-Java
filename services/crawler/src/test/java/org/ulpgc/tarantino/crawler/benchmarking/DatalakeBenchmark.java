package org.ulpgc.tarantino.crawler.benchmarking;

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
public class DatalakeBenchmark {

    @Param({"time", "book", "batch"})
    public String layout;

    @Param({"100", "1000", "10000"})
    public int books;

    @Benchmark
    public void writeThroughput() {
        throw new UnsupportedOperationException("Not implemented yet: split and save every cached book");
    }

    @Benchmark
    public void lookup() {
        throw new UnsupportedOperationException("Not implemented yet: locate header and body of a random stored book");
    }

    @Benchmark
    public void newBooksDetection() {
        throw new UnsupportedOperationException("Not implemented yet: find books stored after a given point in time");
    }
}
