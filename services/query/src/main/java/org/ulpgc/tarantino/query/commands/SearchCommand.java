package org.ulpgc.tarantino.query.commands;

import org.ulpgc.tarantino.query.model.SearchResult;
import org.ulpgc.tarantino.query.ports.InvertedIndexReader;
import org.ulpgc.tarantino.query.ports.MetadataReader;

public class SearchCommand {

    private final InvertedIndexReader invertedIndex;
    private final MetadataReader metadata;

    public SearchCommand(InvertedIndexReader invertedIndex, MetadataReader metadata) {
        this.invertedIndex = invertedIndex;
        this.metadata = metadata;
    }

    public SearchResult execute(String query) {
        // TODO: normalize terms with the indexer's rules, intersect postings, resolve metadata
        throw new UnsupportedOperationException("TODO");
    }
}
