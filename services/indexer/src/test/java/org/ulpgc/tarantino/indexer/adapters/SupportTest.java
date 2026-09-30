package org.ulpgc.tarantino.indexer.adapters;

import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.indexer.model.TermOccurrences;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportTest {

    @Test
    void termFilesUseTheFirstCodePointAsDirectory() {
        Path root = Path.of("index");

        assertEquals(root.resolve("w/whale.txt"), TermFiles.file(root, "whale"));
        assertEquals(root.resolve("𝒜/𝒜b.txt"), TermFiles.file(root, "𝒜b"));
    }

    @Test
    void pendingPostingsAreEmptiedWhenDrained() {
        PendingPostings pending = new PendingPostings();
        pending.add(new TermOccurrences(5, Map.of("island", 1)));
        pending.add(new TermOccurrences(1342, Map.of("island", 2)));

        assertEquals(Map.of("island", Set.of(5, 1342)), pending.drain());
        assertTrue(pending.drain().isEmpty());
    }

    @Test
    void mongoDatabaseComesFromTheUriOrDefaultsToTarantino() {
        assertEquals("tarantino", MongoDatabases.database("mongodb://localhost:27017").getName());
        assertEquals("tarantino_benchmark", MongoDatabases.database("mongodb://localhost:27017/tarantino_benchmark").getName());
    }

    @Test
    void portablePathsUseForwardSlashes() {
        assertEquals("datalake/20250925/14/5.body.txt", PortablePaths.of(Path.of("datalake", "20250925", "14", "5.body.txt")));
    }
}
