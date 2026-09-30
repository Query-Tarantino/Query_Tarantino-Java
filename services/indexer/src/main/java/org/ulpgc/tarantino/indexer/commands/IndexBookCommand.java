package org.ulpgc.tarantino.indexer.commands;

import org.ulpgc.tarantino.indexer.model.BookText;
import org.ulpgc.tarantino.indexer.model.HeaderParser;
import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.model.Tokenizer;
import org.ulpgc.tarantino.indexer.ports.DatalakeReader;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

public class IndexBookCommand {

    private final DatalakeReader datalake;
    private final HeaderParser headerParser;
    private final Tokenizer tokenizer;
    private final InvertedIndexStorage invertedIndex;
    private final MetadataStorage metadata;

    public IndexBookCommand(DatalakeReader datalake, HeaderParser headerParser, Tokenizer tokenizer,
                            InvertedIndexStorage invertedIndex, MetadataStorage metadata) {
        this.datalake = datalake;
        this.headerParser = headerParser;
        this.tokenizer = tokenizer;
        this.invertedIndex = invertedIndex;
        this.metadata = metadata;
    }

    public IndexResult execute(int bookId) {
        return datalake.bookText(bookId)
                .map(this::index)
                .orElseGet(() -> IndexResult.notFound(bookId));
    }

    private IndexResult index(BookText text) {
        metadata.save(headerParser.book(text));
        TermOccurrences occurrences = tokenizer.occurrences(text.bookId(), text.body());
        store(occurrences);
        return IndexResult.success(text.bookId(), occurrences.frequencies().size());
    }

    private void store(TermOccurrences occurrences) {
        invertedIndex.add(occurrences);
        invertedIndex.flush();
    }
}
