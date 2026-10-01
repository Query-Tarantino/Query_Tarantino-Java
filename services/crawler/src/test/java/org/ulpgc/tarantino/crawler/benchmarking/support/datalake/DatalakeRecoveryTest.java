package org.ulpgc.tarantino.crawler.benchmarking.support.datalake;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.ulpgc.tarantino.crawler.ports.BookDownloader;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatalakeRecoveryTest {

    private static final BookDownloader SYNTHETIC_BOOKS = bookId ->
            "Title: Book " + bookId + "\n*** START OF THE PROJECT GUTENBERG EBOOK X ***\nbody " + bookId + "\n*** END OF THE PROJECT GUTENBERG EBOOK X ***";

    @TempDir
    Path root;

    @ParameterizedTest
    @ValueSource(strings = {"time", "book", "batch"})
    void resumesAfterInterruptionWithoutDuplicatesOrLosses(String layout) {
        List<Integer> ids = IntStream.rangeClosed(1, 20).boxed().toList();

        assertTrue(new RecoveryScenario(layout, root, SYNTHETIC_BOOKS).run(ids).recovered());
    }

    @ParameterizedTest
    @CsvSource({"time, 2", "book, 0", "batch, 0"})
    void countsTheFilesLeftByTheInterruption(String layout, long leftoverFiles) {
        List<Integer> ids = IntStream.rangeClosed(1, 20).boxed().toList();

        // time resumes in a new hour directory, leaving the orphaned header and the .tmp body in the old one
        assertEquals(leftoverFiles, new RecoveryScenario(layout, root, SYNTHETIC_BOOKS).run(ids).leftoverFiles());
    }
}
