package org.ulpgc.tarantino.control.adapters;

import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.control.model.Outcome;
import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;
import org.ulpgc.tarantino.crawler.model.book.StoredPaths;
import org.ulpgc.tarantino.crawler.model.failure.DownloadException;
import org.ulpgc.tarantino.crawler.model.failure.FailureReason;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;
import org.ulpgc.tarantino.indexer.commands.IndexBookCommand;
import org.ulpgc.tarantino.indexer.model.book.BookText;
import org.ulpgc.tarantino.indexer.model.book.HeaderParser;
import org.ulpgc.tarantino.indexer.model.terms.TermOccurrences;
import org.ulpgc.tarantino.indexer.model.terms.Tokenizer;
import org.ulpgc.tarantino.indexer.ports.datamarts.InvertedIndexStorage;
import org.ulpgc.tarantino.indexer.ports.sources.DatalakeReader;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LocalAdaptersTest {

    private static final StoredPaths STORED = new StoredPaths(Path.of("datalake/5/header.txt"), Path.of("datalake/5/body.txt"));

    @Test
    void crawlerReportsWhereTheBookWasStored() {
        assertEquals(Outcome.success("stored in datalake/5"), new LocalCrawler(ingest(STORED)).ingest(5));
    }

    @Test
    void crawlerReportsTheFailureReason() {
        assertEquals(Outcome.failure("skipped, NOT_FOUND"), new LocalCrawler(ingest(null)).ingest(5));
    }

    @Test
    void indexerReportsTheUniqueTermsOfEachBookAndTheOnesMissingFromTheDatalake() {
        BookText text = new BookText(5, "Title: T", "island whale island", Path.of("5.body.txt"));
        LocalIndexer indexer = new LocalIndexer(index(bookId -> Optional.of(text).filter(found -> bookId == 5)));

        assertEquals(Map.of(5, Outcome.success("2 unique terms indexed"), 6, Outcome.failure("skipped, not found in the datalake")),
                indexer.index(List.of(5, 6)));
    }

    private static IngestBookCommand ingest(StoredPaths stored) {
        DatalakeStorage datalake = new DatalakeStorage() {
            @Override
            public StoredPaths save(org.ulpgc.tarantino.crawler.model.book.BookText book) {
                throw new AssertionError("Nothing must be saved");
            }

            @Override
            public Optional<StoredPaths> pathsOf(int bookId) {
                return Optional.ofNullable(stored);
            }

            @Override
            public Set<Integer> idsStoredSince(Instant instant) {
                return Set.of();
            }

            @Override
            public int removeIncompleteWrites() {
                return 0;
            }
        };
        return new IngestBookCommand(bookId -> {
            throw new DownloadException(FailureReason.NOT_FOUND, "missing");
        }, datalake);
    }

    private static IndexBookCommand index(DatalakeReader datalake) {
        InvertedIndexStorage index = new InvertedIndexStorage() {
            @Override
            public void add(TermOccurrences occurrences) {
            }

            @Override
            public void flush() {
            }
        };
        return new IndexBookCommand(datalake, new HeaderParser(), new Tokenizer(Set.of()), index, book -> {
        });
    }
}
