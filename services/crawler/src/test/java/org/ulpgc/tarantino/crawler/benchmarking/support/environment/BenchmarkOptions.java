package org.ulpgc.tarantino.crawler.benchmarking.support.environment;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public final class BenchmarkOptions {

    private static final TimeValue QUICK_ITERATION = TimeValue.milliseconds(200);

    private BenchmarkOptions() {
    }

    public static ChainedOptionsBuilder forService(String service) {
        ChainedOptionsBuilder options = new OptionsBuilder()
                .include("org\\.ulpgc\\.tarantino\\." + service + "\\.benchmarking\\..*")
                .resultFormat(ResultFormatType.CSV)
                .result(rawResultsFile(service));
        applyOverrides(options);
        return options;
    }

    private static void applyOverrides(ChainedOptionsBuilder options) {
        variable("TARANTINO_BENCHMARK_BOOKS").ifPresent(books -> options.param("books", books.split(",")));
        variable("TARANTINO_BENCHMARK_QUICK").filter(Boolean::parseBoolean).ifPresent(quick -> quick(options));
        variable("TARANTINO_BENCHMARK_SKIP_MONGO").filter(Boolean::parseBoolean).ifPresent(skip -> withoutMongo(options));
    }

    private static String rawResultsFile(String service) {
        Path directory = BenchmarkPaths.benchmarks().resolve(service);
        try {
            Files.createDirectories(directory);
            return directory.resolve("jmh-results.csv").toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void quick(ChainedOptionsBuilder options) {
        options.warmupIterations(1).measurementIterations(1).warmupTime(QUICK_ITERATION).measurementTime(QUICK_ITERATION);
    }

    private static void withoutMongo(ChainedOptionsBuilder options) {
        options.param("index", "json", "folders").param("metadata", "sqlite");
    }

    private static Optional<String> variable(String name) {
        return Optional.ofNullable(System.getenv(name)).filter(value -> !value.isBlank());
    }
}
