package org.ulpgc.tarantino.indexer.benchmarking;

import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.ulpgc.tarantino.crawler.benchmarking.support.BenchmarkOptions;
import org.ulpgc.tarantino.crawler.benchmarking.support.FootprintLog;
import org.ulpgc.tarantino.crawler.benchmarking.support.JmhResults;
import org.ulpgc.tarantino.crawler.benchmarking.support.Metric;
import org.ulpgc.tarantino.crawler.benchmarking.support.ResultRow;
import org.ulpgc.tarantino.crawler.benchmarking.support.ResultsFile;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class BenchmarkRunner {

    static final String SERVICE = "indexer";

    private static final Map<String, Metric> METRICS = Map.of(
            "fullBuildTime", Metric.asMeasured("full_build_time", "ms"),
            JmhResults.secondary("fullBuildTime", "gc.alloc.rate.norm"), Metric.asMeasured("memory_allocated", "bytes"),
            "incrementalUpdateTime", Metric.asMeasured("incremental_update_time", "ms"),
            "bulkInsertionTime", Metric.asMeasured("bulk_insertion_time", "ms"));

    public static void main(String[] args) throws RunnerException {
        FootprintLog.drain(SERVICE);
        Collection<RunResult> results = new Runner(BenchmarkOptions.forService(SERVICE).addProfiler(GCProfiler.class).build()).run();
        System.out.println("Results written to " + ResultsFile.write(SERVICE, rows(results)));
    }

    private static List<ResultRow> rows(Collection<RunResult> results) {
        return Stream.of(JmhResults.rows(results, METRICS), FootprintLog.drain(SERVICE)).flatMap(List::stream).toList();
    }
}
