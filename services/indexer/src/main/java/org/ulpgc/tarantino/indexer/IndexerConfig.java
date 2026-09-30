package org.ulpgc.tarantino.indexer;

import java.nio.file.Path;

public record IndexerConfig(Path datalake, String datalakeLayout, Path datamarts, String index,
                            String metadata, String mongoUri, Path workload) {

    public static IndexerConfig fromEnvironment() {
        return new IndexerConfig(
                Path.of(env("TARANTINO_DATALAKE", "datalake")),
                env("TARANTINO_DATALAKE_LAYOUT", "time"),
                Path.of(env("TARANTINO_DATAMARTS", "datamarts")),
                env("TARANTINO_INDEX", "json"),
                env("TARANTINO_METADATA", "sqlite"),
                env("TARANTINO_MONGO_URI", "mongodb://localhost:27017"),
                Path.of(env("TARANTINO_WORKLOAD", "workload")));
    }

    private static String env(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue);
    }
}
