package org.ulpgc.tarantino.control;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ControlConfigTest {

    @Test
    void readsAPositiveIndexBatch() {
        assertEquals(100, ControlConfig.indexBatch("100"));
        assertEquals(1, ControlConfig.indexBatch(" 1 "));
    }

    @Test
    void rejectsAnIndexBatchThatIsNotAPositiveInteger() {
        assertThrows(IllegalArgumentException.class, () -> ControlConfig.indexBatch("0"));
        assertThrows(IllegalArgumentException.class, () -> ControlConfig.indexBatch("many"));
    }
}
