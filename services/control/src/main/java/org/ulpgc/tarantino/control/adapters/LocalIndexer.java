package org.ulpgc.tarantino.control.adapters;

import org.ulpgc.tarantino.control.model.Outcome;
import org.ulpgc.tarantino.control.ports.Indexer;
import org.ulpgc.tarantino.indexer.commands.IndexBookCommand;
import org.ulpgc.tarantino.indexer.commands.IndexResult;

public class LocalIndexer implements Indexer {

    private final IndexBookCommand index;

    public LocalIndexer(IndexBookCommand index) {
        this.index = index;
    }

    @Override
    public Outcome index(int bookId) {
        return outcome(index.execute(bookId));
    }

    private static Outcome outcome(IndexResult result) {
        return result.indexed()
                ? Outcome.success(result.uniqueTerms() + " unique terms indexed")
                : Outcome.failure("skipped, not found in the datalake");
    }
}
