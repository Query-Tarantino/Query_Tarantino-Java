package org.ulpgc.tarantino.indexer.ports;

import org.ulpgc.tarantino.indexer.model.TermOccurrences;

public interface InvertedIndexStorage {

    void add(TermOccurrences occurrences);

    void flush();
}
