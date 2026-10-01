package org.ulpgc.tarantino.crawler.benchmarking.support.results;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class JmhResults {

    private static final List<String> STRUCTURE_PARAMS = List.of("layout", "index", "metadata");

    private JmhResults() {
    }

    public static List<ResultRow> rows(Collection<RunResult> results, Map<String, Metric> metrics) {
        return results.stream().map(result -> mappedRow(result, metrics)).flatMap(Optional::stream).toList();
    }

    private static Optional<ResultRow> mappedRow(RunResult result, Map<String, Metric> metrics) {
        BenchmarkParams params = result.getParams();
        return Optional.ofNullable(metrics.get(method(params))).map(metric -> row(params, metric, result.getPrimaryResult()));
    }

    private static ResultRow row(BenchmarkParams params, Metric metric, Result<?> result) {
        return metric.row(structure(params), Integer.parseInt(params.getParam("books")), result.getScore(), result.getScoreError());
    }

    private static String structure(BenchmarkParams params) {
        return STRUCTURE_PARAMS.stream().map(params::getParam).filter(Objects::nonNull).findFirst().orElseThrow();
    }

    private static String method(BenchmarkParams params) {
        return params.getBenchmark().substring(params.getBenchmark().lastIndexOf('.') + 1);
    }
}
