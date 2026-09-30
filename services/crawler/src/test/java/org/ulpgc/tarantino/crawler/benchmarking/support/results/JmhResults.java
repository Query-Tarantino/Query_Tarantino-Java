package org.ulpgc.tarantino.crawler.benchmarking.support.results;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.RunResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public final class JmhResults {

    private static final List<String> STRUCTURE_PARAMS = List.of("layout", "index", "metadata");
    private static final String SECONDARY_SEPARATOR = ":";

    private JmhResults() {
    }

    public static List<ResultRow> rows(Collection<RunResult> results, Map<String, Metric> metrics) {
        return results.stream().flatMap(result -> rows(result, metrics)).toList();
    }

    public static String secondary(String method, String label) {
        return method + SECONDARY_SEPARATOR + label;
    }

    private static Stream<ResultRow> rows(RunResult result, Map<String, Metric> metrics) {
        String method = method(result.getParams());
        Stream<Map.Entry<String, Double>> primary = Stream.of(Map.entry(method, result.getPrimaryResult().getScore()));
        return Stream.concat(primary, secondaryScores(method, result))
                .map(entry -> mappedRow(result.getParams(), metrics.get(entry.getKey()), entry.getValue()))
                .flatMap(Optional::stream);
    }

    private static Stream<Map.Entry<String, Double>> secondaryScores(String method, RunResult result) {
        return result.getSecondaryResults().keySet().stream()
                .map(label -> Map.entry(secondary(method, label.replace("·", "")), result.getSecondaryResults().get(label).getScore()));
    }

    private static Optional<ResultRow> mappedRow(BenchmarkParams params, Metric metric, double score) {
        return Optional.ofNullable(metric).map(found -> row(params, found, score));
    }

    private static ResultRow row(BenchmarkParams params, Metric metric, double score) {
        int books = Integer.parseInt(params.getParam("books"));
        return new ResultRow(structure(params), metric.name(), books, metric.conversion().value(score, books), metric.unit());
    }

    private static String structure(BenchmarkParams params) {
        return STRUCTURE_PARAMS.stream().map(params::getParam).filter(Objects::nonNull).findFirst().orElseThrow();
    }

    private static String method(BenchmarkParams params) {
        return params.getBenchmark().substring(params.getBenchmark().lastIndexOf('.') + 1);
    }
}
