package org.ulpgc.tarantino.query.benchmarking;

import org.ulpgc.tarantino.crawler.benchmarking.support.environment.BenchmarkPaths;
import org.ulpgc.tarantino.indexer.benchmarking.support.BenchmarkStore;
import org.ulpgc.tarantino.query.QueryConfig;
import org.ulpgc.tarantino.query.QueryFactory;
import org.ulpgc.tarantino.query.adapters.stopwords.FileStopwordsLoader;
import org.ulpgc.tarantino.query.commands.SearchCommand;
import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

final class QueryWorkload {

    // Metadata is not read by the index benchmarks, so only the index is measured
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

    private QueryWorkload() {
    }

    static SearchCommand openSearch(BenchmarkStore store, String index, Set<String> stopwords) {
        QueryConfig config = new QueryConfig(store.datamarts(), index, "sqlite", store.mongoUri(), BenchmarkPaths.workload());
        return new SearchCommand(QueryFactory.invertedIndex(config), CONSTANT_METADATA, stopwords);
    }

    static Set<String> stopwords() {
        return new FileStopwordsLoader(BenchmarkPaths.workload().resolve("stopwords.txt")).stopwords();
    }

    static List<String> queries() {
        try {
            return Files.readAllLines(BenchmarkPaths.workload().resolve("queries.txt")).stream()
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
