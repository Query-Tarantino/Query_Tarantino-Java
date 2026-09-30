package org.ulpgc.tarantino.indexer.commands;

import org.ulpgc.tarantino.indexer.model.HeaderParser;
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
        // TODO: datalake.read -> headerParser.parse -> metadata.save; tokenizer.tokenize -> invertedIndex.add
        throw new UnsupportedOperationException("TODO");
    }
}
