package org.ulpgc.tarantino.control.ports;

import org.ulpgc.tarantino.control.model.Outcome;

public interface Crawler {

    Outcome ingest(int bookId);
}
