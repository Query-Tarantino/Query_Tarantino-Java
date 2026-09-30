package org.ulpgc.tarantino.indexer.ports;

import java.util.Set;

public interface StopwordsLoader {

    Set<String> load();
}
