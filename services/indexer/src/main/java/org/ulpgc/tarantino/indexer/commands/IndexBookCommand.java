package org.ulpgc.tarantino.indexer.commands;

import org.ulpgc.tarantino.indexer.model.BookText;
import org.ulpgc.tarantino.indexer.model.HeaderParser;
import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.model.Tokenizer;
import org.ulpgc.tarantino.indexer.ports.DatalakeReader;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

import java.util.Optional;

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

    /** Idempotent: re-indexing a book overwrites its metadata and adds no duplicate postings. */
    public IndexResult execute(int bookId) {
        Optional<BookText> text = datalake.read(bookId);
        if (text.isEmpty()) {
            return new IndexResult(bookId, false, 0);
        }
        metadata.save(headerParser.parse(text.get()));
        TermOccurrences occurrences = tokenizer.tokenize(bookId, text.get().body());
        invertedIndex.add(occurrences);
        invertedIndex.flush();
        return new IndexResult(bookId, true, occurrences.frequencies().size());
    }
}
