package org.ulpgc.tarantino.indexer.adapters.index.folders;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TermFilesTest {

    @Test
    void useTheFirstCodePointAsDirectory() {
        Path root = Path.of("index");

        assertEquals(root.resolve("w/whale.txt"), TermFiles.file(root, "whale"));
        assertEquals(root.resolve("𝒜/𝒜b.txt"), TermFiles.file(root, "𝒜b"));
    }
}
