package org.ulpgc.tarantino.crawler;

import java.nio.file.Path;

public record CrawlerConfig(Path datalake, String datalakeLayout) {

    public static CrawlerConfig fromEnvironment() {
        return new CrawlerConfig(
                Path.of(variable("TARANTINO_DATALAKE", "datalake")),
                variable("TARANTINO_DATALAKE_LAYOUT", "time"));
    }

    private static String variable(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue);
    }
}
