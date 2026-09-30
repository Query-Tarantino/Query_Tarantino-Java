package org.ulpgc.tarantino.indexer.commands;

import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.indexer.model.Book;
import org.ulpgc.tarantino.indexer.model.BookText;
import org.ulpgc.tarantino.indexer.model.HeaderParser;
import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.model.Tokenizer;
import org.ulpgc.tarantino.indexer.ports.DatalakeReader;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndexBookCommandTest {

    private static final Path BODY = Path.of("datalake/1342/body.txt");

    private final List<Book> savedBooks = new ArrayList<>();
    private final RecordingIndex index = new RecordingIndex();

    @Test
    void savesMetadataAndFlushesTermsOfStoredBooks() {
        BookText text = new BookText(1342, "Title: Pride\nAuthor: Austen", "The island, the island!", BODY);

        IndexResult result = command(bookId -> Optional.of(text)).execute(1342);

        assertEquals(IndexResult.success(1342, 1), result);
        assertEquals(List.of(new Book(1342, "Pride", "Austen", null, BODY)), savedBooks);
        assertEquals(List.of(new TermOccurrences(1342, Map.of("island", 2))), index.added);
        assertEquals(1, index.flushes);
    }

    @Test
    void reportsBooksMissingFromTheDatalakeWithoutWriting() {
        assertEquals(IndexResult.notFound(7), command(bookId -> Optional.empty()).execute(7));
        assertTrue(savedBooks.isEmpty());
        assertTrue(index.added.isEmpty());
    }

    private IndexBookCommand command(DatalakeReader datalake) {
        return new IndexBookCommand(datalake, new HeaderParser(), new Tokenizer(Set.of("the")), index, savedBooks::add);
    }

    private static class RecordingIndex implements InvertedIndexStorage {

        private final List<TermOccurrences> added = new ArrayList<>();
        private int flushes;

        @Override
        public void add(TermOccurrences occurrences) {
            added.add(occurrences);
        }

        @Override
        public void flush() {
            flushes++;
        }
    }
}
