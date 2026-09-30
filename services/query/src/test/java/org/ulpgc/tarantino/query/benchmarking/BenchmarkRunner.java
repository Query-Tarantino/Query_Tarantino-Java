package org.ulpgc.tarantino.query.benchmarking;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.nio.file.Files;
import java.nio.file.Path;

/** Entry point for {@code mvn test -Pbenchmark}; writes results to {@code benchmarks/query/}. */
public class BenchmarkRunner {

    public static void main(String[] args) throws Exception {
        Path output = Path.of(System.getenv().getOrDefault("TARANTINO_BENCHMARKS", "benchmarks"), "query");
        Files.createDirectories(output);

        // TODO: convert jmh-results.csv to the shared format: language,structure,metric,n_books,value
        Options options = new OptionsBuilder()
                .include("org.ulpgc.tarantino.query.benchmarking.")
                .addProfiler(GCProfiler.class)
                .resultFormat(ResultFormatType.CSV)
                .result(output.resolve("jmh-results.csv").toString())
                .build();
        new Runner(options).run();
    }
}
