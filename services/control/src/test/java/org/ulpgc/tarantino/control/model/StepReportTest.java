package org.ulpgc.tarantino.control.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StepReportTest {

    @Test
    void describesTheActionBookAndOutcome() {
        StepReport report = new StepReport(NextStep.download(1342), Outcome.success("stored in datalake/1342"));

        assertEquals("DOWNLOAD 1342: stored in datalake/1342", report.description());
        assertFalse(report.idle());
    }

    @Test
    void isIdleOnlyForIdleSteps() {
        assertTrue(new StepReport(NextStep.idle(), Outcome.success("nothing left to do")).idle());
    }
}
