package org.ulpgc.tarantino.query.benchmarking;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.ulpgc.tarantino.crawler.benchmarking.support.environment.BenchmarkOptions;
import org.ulpgc.tarantino.crawler.benchmarking.support.files.ResultsFile;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.JmhResults;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.Metric;

import java.util.Collection;
import java.util.Map;

public class BenchmarkRunner {

    private static final String SERVICE = "query";
    private static final Map<String, Metric> METRICS = Map.of(
            "queryTime", Metric.asMeasured("query_time", "µs/query"),
            "bookByIdTime", Metric.asMeasured("book_by_id_time", "µs/op"),
            "booksByAuthorTime", Metric.asMeasured("books_by_author_time", "µs/op"));

    public static void main(String[] args) throws RunnerException {
        Collection<RunResult> results = new Runner(BenchmarkOptions.forService(SERVICE).build()).run();
        System.out.println("Results written to " + ResultsFile.write(SERVICE, JmhResults.rows(results, METRICS)));
    }
}
