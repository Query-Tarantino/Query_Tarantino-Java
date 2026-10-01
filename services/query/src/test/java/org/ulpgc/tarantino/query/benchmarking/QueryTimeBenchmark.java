package org.ulpgc.tarantino.query.benchmarking;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.ulpgc.tarantino.crawler.benchmarking.support.environment.Heap;
import org.ulpgc.tarantino.crawler.benchmarking.support.files.FootprintLog;
import org.ulpgc.tarantino.crawler.benchmarking.support.results.ResultRow;
import org.ulpgc.tarantino.crawler.benchmarking.support.validation.Check;
import org.ulpgc.tarantino.indexer.benchmarking.support.BenchmarkStore;
import org.ulpgc.tarantino.indexer.benchmarking.support.IndexFixture;
import org.ulpgc.tarantino.indexer.benchmarking.support.PrebuiltIndexes;
import org.ulpgc.tarantino.query.commands.SearchCommand;
import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.model.QueryTerms;
import org.ulpgc.tarantino.query.model.SearchResult;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 3, jvmArgsAppend = {"-Xmx4g", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"})
public class QueryTimeBenchmark {

    // The first opening of each process is discarded: for mongo it also creates the client
    private static final int INDEX_MEMORY_SAMPLES = 5;

    @Param({"json", "folders", "mongo"})
    public String index;

    @Param({"100", "500", "1000", "2000"})
    public int books;

    // all: every query of queries.txt, also reported as the 99th percentile; the others, one category each
    @Param({QueryWorkload.ALL_CATEGORIES, "frequent", "rare", "mixed", "long", "empty", "nonascii"})
    public String category;

    private BenchmarkStore store;
    private SearchCommand search;
    private Set<String> stopwords;
    private List<String> queries;
    private List<String> measured;

    @Setup(Level.Trial)
    public void openIndex() {
        IndexFixture fixture = IndexFixture.fromEnvironment();
        store = PrebuiltIndexes.of(index, books, fixture);
        stopwords = QueryWorkload.stopwords();
        List<WorkloadQuery> workload = QueryWorkload.queries();
        queries = QueryWorkload.texts(workload, QueryWorkload.ALL_CATEGORIES);
        measured = QueryWorkload.texts(workload, category);
        Check.require(!measured.isEmpty(), "no query of category " + category + " in queries.txt");
        search = openAndAnswerEveryQuery();
        if (QueryWorkload.ALL_CATEGORIES.equals(category)) {
            for (int sample = 0; sample < INDEX_MEMORY_SAMPLES; sample++) {
                recordIndexMemory();
            }
        }
        requireReferenceResults(fixture);
    }

    @Benchmark
    public SearchResult queryTime() {
        return search.execute(measured.get(ThreadLocalRandom.current().nextInt(measured.size())));
    }

    private void recordIndexMemory() {
        search = null;
        long before = Heap.usedAfterFullCollection();
        search = openAndAnswerEveryQuery();
        long indexMemory = Heap.usedAfterFullCollection() - before;
        FootprintLog.appendSample(BenchmarkRunner.SERVICE, ResultRow.sample(index, "index_memory", books, indexMemory, "bytes"));
    }

    private SearchCommand openAndAnswerEveryQuery() {
        SearchCommand opened = QueryWorkload.openSearch(store, index, stopwords);
        queries.forEach(opened::execute);
        return opened;
    }

    private void requireReferenceResults(IndexFixture fixture) {
        Map<String, List<Integer>> expected = referenceResults(fixture);
        queries.forEach(query -> Check.require(resultIds(query).equals(expected.get(query)), "wrong result for query '" + query + "'"));
    }

    private Map<String, List<Integer>> referenceResults(IndexFixture fixture) {
        Path cached = PrebuiltIndexes.file("reference-results-" + books + ".tsv");
        if (Files.exists(cached)) {
            return ReferenceResults.read(cached);
        }
        Map<String, List<Integer>> expected = computedReferenceResults(fixture);
        ReferenceResults.write(cached, expected);
        return expected;
    }

    private Map<String, List<Integer>> computedReferenceResults(IndexFixture fixture) {
        Map<String, Set<String>> termsOfQuery = new HashMap<>();
        Map<String, List<Integer>> expected = new HashMap<>();
        queries.forEach(query -> {
            termsOfQuery.put(query, QueryTerms.of(query, stopwords));
            expected.put(query, new ArrayList<>());
        });
        for (int bookId : fixture.dataset().ids(books).stream().sorted().toList()) {
            Set<String> bookTerms = fixture.terms(bookId);
            termsOfQuery.forEach((query, terms) -> {
                if (!terms.isEmpty() && bookTerms.containsAll(terms)) {
                    expected.get(query).add(bookId);
                }
            });
        }
        return expected;
    }

    private List<Integer> resultIds(String query) {
        return search.execute(query).books().stream().map(BookMetadata::bookId).toList();
    }
}
