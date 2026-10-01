package org.ulpgc.tarantino.indexer.benchmarking;

import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.ulpgc.tarantino.crawler.benchmarking.support.environment.BenchmarkOptions;
import org.ulpgc.tarantino.crawler.benchmarking.support.files.FootprintLog;
import org.ulpgc.tarantino.crawler.benchmarking.support.files.ResultsFile;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.JmhResults;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.Metric;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.ResultRow;
import org.ulpgc.tarantino.indexer.benchmarking.support.PrebuiltIndexes;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class BenchmarkRunner {

    public static final String SERVICE = "indexer";

    private static final Map<String, Metric> METRICS = Map.of(
            "fullBuildTime", Metric.asMeasured("full_build_time", "ms"),
            "incrementalUpdateTime", Metric.asMeasured("incremental_update_time", "ms"),
            "bulkInsertionTime", Metric.asMeasured("bulk_insertion_time", "ms"));

    public static void main(String[] args) throws RunnerException {
        FootprintLog.drain(SERVICE);
        PrebuiltIndexes.deleteAll();
        try {
            Collection<RunResult> results = new Runner(BenchmarkOptions.forService(SERVICE).build()).run();
            System.out.println("Results written to " + ResultsFile.write(SERVICE, rows(results)));
        } finally {
            PrebuiltIndexes.deleteAll();
        }
    }

    private static List<ResultRow> rows(Collection<RunResult> results) {
        return Stream.of(JmhResults.rows(results, METRICS), FootprintLog.drain(SERVICE)).flatMap(List::stream).toList();
    }
}
