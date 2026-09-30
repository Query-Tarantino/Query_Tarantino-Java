package org.ulpgc.tarantino.indexer.commands;

import org.ulpgc.tarantino.indexer.model.book.BookText;
import org.ulpgc.tarantino.indexer.model.book.HeaderParser;
import org.ulpgc.tarantino.indexer.model.terms.TermOccurrences;
import org.ulpgc.tarantino.indexer.model.terms.Tokenizer;
import org.ulpgc.tarantino.indexer.ports.datamarts.InvertedIndexStorage;
import org.ulpgc.tarantino.indexer.ports.datamarts.MetadataStorage;
import org.ulpgc.tarantino.indexer.ports.sources.DatalakeReader;

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
