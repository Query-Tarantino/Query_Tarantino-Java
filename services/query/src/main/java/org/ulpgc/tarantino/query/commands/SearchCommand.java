package org.ulpgc.tarantino.query.commands;

import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.model.QueryTerms;
import org.ulpgc.tarantino.query.model.SearchResult;
import org.ulpgc.tarantino.query.ports.InvertedIndexReader;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class SearchCommand {

    private final InvertedIndexReader invertedIndex;
    private final MetadataReader metadata;
    private final Set<String> stopwords;

    public SearchCommand(InvertedIndexReader invertedIndex, MetadataReader metadata, Set<String> stopwords) {
        this.invertedIndex = invertedIndex;
        this.metadata = metadata;
        this.stopwords = stopwords;
    }

    /** Returns the books that contain every term of the query (AND semantics), ordered by id. */
    public SearchResult execute(String query) {
        Set<String> terms = QueryTerms.of(query, stopwords);
        if (terms.isEmpty()) {
            return new SearchResult(query, List.of());
        }
        TreeSet<Integer> matches = null;
        for (String term : terms) {
            Set<Integer> postings = invertedIndex.postings(term);
            if (matches == null) {
                matches = new TreeSet<>(postings);
            } else {
                matches.retainAll(postings);
            }
            if (matches.isEmpty()) {
                break;
            }
        }
        List<BookMetadata> books = matches.stream()
                .map(metadata::findById)
                .flatMap(Optional::stream)
                .toList();
        return new SearchResult(query, books);
    }
}
