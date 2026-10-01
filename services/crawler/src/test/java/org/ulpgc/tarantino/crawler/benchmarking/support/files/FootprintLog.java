package org.ulpgc.tarantino.crawler.benchmarking.support.files;

import org.ulpgc.tarantino.crawler.benchmarking.support.environment.BenchmarkPaths;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.ResultRow;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FootprintLog {

    private FootprintLog() {
    }

    public static void append(String service, List<ResultRow> rows) {
        try {
            Files.createDirectories(file(service).getParent());
            Files.write(file(service), rows.stream().map(ResultRow::csvLine).toList(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static List<ResultRow> drain(String service) {
        try {
            List<ResultRow> rows = rows(file(service));
            Files.deleteIfExists(file(service));
            return rows;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<ResultRow> rows(Path file) throws IOException {
        if (!Files.exists(file)) {
            return List.of();
        }
        Map<String, ResultRow> firstPerMeasure = new LinkedHashMap<>();
        Files.readAllLines(file).stream().map(ResultRow::parse).forEach(row -> firstPerMeasure.putIfAbsent(measure(row), row));
        return List.copyOf(firstPerMeasure.values());
    }

    private static String measure(ResultRow row) {
        return String.join(",", row.structure(), row.metric(), String.valueOf(row.books()));
    }

    private static Path file(String service) {
        return BenchmarkPaths.scratch("footprint-" + service + ".csv");
    }
}
