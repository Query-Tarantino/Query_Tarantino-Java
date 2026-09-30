package org.ulpgc.tarantino.crawler.benchmarking;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/** Recovery behavior (PDF 3.1): functional check, not a timing benchmark. */
class DatalakeRecoveryTest {

    @Test
    @Disabled("TODO")
    void resumesAfterInterruptionWithoutDuplicatesOrLosses() {
        // TODO: for each layout, interrupt ingestion mid-batch, resume, assert every id stored exactly once
    }
}
