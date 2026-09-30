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
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.ulpgc.tarantino.crawler.benchmarking.support.BenchmarkPaths;
import org.ulpgc.tarantino.indexer.benchmarking.support.BenchmarkStore;
import org.ulpgc.tarantino.indexer.benchmarking.support.IndexFixture;
import org.ulpgc.tarantino.query.QueryConfig;
import org.ulpgc.tarantino.query.QueryFactory;
import org.ulpgc.tarantino.query.adapters.FileStopwordsLoader;
import org.ulpgc.tarantino.query.commands.SearchCommand;
import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.model.SearchResult;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1, jvmArgsAppend = {"-Xmx4g", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow"})
public class QueryTimeBenchmark {

    private static final MetadataReader CONSTANT_METADATA = new MetadataReader() {
        @Override
        public Optional<BookMetadata> book(int bookId) {
            return Optional.of(new BookMetadata(bookId, "title", "author", "English", Path.of("body.txt")));
        }

        @Override
        public List<BookMetadata> booksBy(String author) {
            return List.of();
        }
    };

    @Param({"json", "folders", "mongo"})
    public String index;

    @Param({"100", "500", "1000", "2000"})
    public int books;

    private BenchmarkStore store;
    private SearchCommand search;
    private List<String> queries;

    @Setup(Level.Trial)
    public void buildIndex() {
        store = BenchmarkStore.forIndex(index, "query-index-" + index + "-" + books);
        store.clear();
        IndexFixture fixture = IndexFixture.fromEnvironment();
        fixture.index(store.invertedIndex(), fixture.dataset().ids(books));
        search = new SearchCommand(QueryFactory.invertedIndex(config()), CONSTANT_METADATA, stopwords());
        queries = queries();
    }

    @Benchmark
    public SearchResult queryTime() {
        return search.execute(queries.get(ThreadLocalRandom.current().nextInt(queries.size())));
    }

    @TearDown(Level.Trial)
    public void deleteIndex() {
        store.clear();
    }

    private QueryConfig config() {
        return new QueryConfig(store.datamarts(), index, "sqlite", store.mongoUri(), BenchmarkPaths.workload());
    }

    private static Set<String> stopwords() {
        return new FileStopwordsLoader(BenchmarkPaths.workload().resolve("stopwords.txt")).stopwords();
    }

    private static List<String> queries() {
        try {
            return Files.readAllLines(BenchmarkPaths.workload().resolve("queries.txt")).stream()
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
