package org.ulpgc.tarantino.control.ports;

import org.ulpgc.tarantino.control.model.Outcome;

public interface Indexer {

    Outcome index(int bookId);
}
