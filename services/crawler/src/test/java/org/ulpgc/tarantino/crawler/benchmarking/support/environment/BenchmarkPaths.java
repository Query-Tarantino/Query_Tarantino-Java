package org.ulpgc.tarantino.crawler.benchmarking.support.environment;

import java.nio.file.Path;

public final class BenchmarkPaths {

    private BenchmarkPaths() {
    }

    public static Path benchmarks() {
        return Path.of(variable("TARANTINO_BENCHMARKS", "benchmarks"));
    }

    public static Path workload() {
        return Path.of(variable("TARANTINO_WORKLOAD", "workload"));
    }

    public static Path scratch(String name) {
        return benchmarks().resolve("tmp").resolve(name);
    }

    public static String mongoUri(String database) {
        return variable("TARANTINO_MONGO_URI", "mongodb://localhost:27017").replaceAll("/+$", "") + "/" + database;
    }

    private static String variable(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue);
    }
}
