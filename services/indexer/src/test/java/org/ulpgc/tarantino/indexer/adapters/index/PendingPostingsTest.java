package org.ulpgc.tarantino.indexer.adapters.index;

import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.indexer.model.terms.TermOccurrences;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PendingPostingsTest {

    @Test
    void areEmptiedWhenDrained() {
        PendingPostings pending = new PendingPostings();
        pending.add(new TermOccurrences(5, Map.of("island", 1)));
        pending.add(new TermOccurrences(1342, Map.of("island", 2)));

        assertEquals(Map.of("island", Set.of(5, 1342)), pending.drain());
        assertTrue(pending.drain().isEmpty());
    }
}
