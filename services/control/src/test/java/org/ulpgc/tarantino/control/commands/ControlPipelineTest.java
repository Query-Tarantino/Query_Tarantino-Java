package org.ulpgc.tarantino.control.commands;

import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.control.model.NextStep;
import org.ulpgc.tarantino.control.model.Outcome;
import org.ulpgc.tarantino.control.model.StepReport;
import org.ulpgc.tarantino.control.ports.ControlStateStore;
import org.ulpgc.tarantino.control.ports.Crawler;
import org.ulpgc.tarantino.control.ports.Indexer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlPipelineTest {

    private static final int MISSING_BOOK = 404;
    private static final int BOOK_BY_BOOK = 1;
    private static final int DEFAULT_BATCH = 100;

    private final InMemoryState state = new InMemoryState();
    private final Crawler crawler = bookId -> bookId == MISSING_BOOK ? Outcome.failure("not found") : Outcome.success("stored");
    private final RecordingIndexer indexer = new RecordingIndexer();

    @Test
    void withBatchesOfOneDownloadsThenIndexesEachCandidateAndSkipsFailures() {
        ControlPipeline pipeline = new ControlPipeline(state, crawler, indexer, List.of(1, MISSING_BOOK, 2), BOOK_BY_BOOK);

        assertEquals(List.of(NextStep.download(1), NextStep.index(List.of(1)), NextStep.download(MISSING_BOOK),
                NextStep.download(2), NextStep.index(List.of(2))), steps(pipeline));
        assertEquals(Set.of(1, 2), state.indexed());
    }

    @Test
    void indexesInBatchesAndTheLastPartialBatchWhenNothingIsLeftToDownload() {
        ControlPipeline pipeline = new ControlPipeline(state, crawler, indexer, List.of(1, 2, 3), 2);

        assertEquals(List.of(NextStep.download(1), NextStep.download(2), NextStep.index(List.of(1, 2)),
                NextStep.download(3), NextStep.index(List.of(3))), steps(pipeline));
        assertEquals(List.of(List.of(1, 2), List.of(3)), indexer.batches);
        assertEquals(Set.of(1, 2, 3), state.indexed());
    }

    @Test
    void resumesByIndexingBooksDownloadedBeforeAnInterruption() {
        state.markDownloaded(7);

        assertEquals(NextStep.index(List.of(7)), new ControlPipeline(state, crawler, indexer, List.of(), DEFAULT_BATCH).nextStep());
    }

    @Test
    void marksTheBooksOfABatchThatSucceedAndGoesOnWithoutTheOnesThatFail() {
        state.markDownloaded(MISSING_BOOK);
        state.markDownloaded(8);
        ControlPipeline pipeline = new ControlPipeline(state, crawler, indexer, List.of(), DEFAULT_BATCH);

        StepReport report = pipeline.runStep();

        assertEquals(Outcome.failure("1 indexed, 1 skipped"), report.outcome());
        assertEquals(Set.of(8), state.indexed());
        assertTrue(pipeline.runStep().idle());
    }

    @Test
    void marksNoBookOfABatchInterruptedBeforeItsFlush() {
        state.markDownloaded(1);
        state.markDownloaded(2);
        Indexer interrupted = bookIds -> {
            throw new IllegalStateException("interrupted while flushing");
        };
        ControlPipeline pipeline = new ControlPipeline(state, crawler, interrupted, List.of(), DEFAULT_BATCH);

        assertThrows(IllegalStateException.class, pipeline::runStep);
        assertEquals(Set.of(), state.indexed());
    }

    @Test
    void skipsCandidatesAlreadyDownloadedOrRepeated() {
        state.markDownloaded(1);
        state.markIndexed(1);

        assertEquals(List.of(NextStep.download(2), NextStep.index(List.of(2))),
                steps(new ControlPipeline(state, crawler, indexer, List.of(1, 2, 1, 2), BOOK_BY_BOOK)));
    }

    @Test
    void readsTheStateOnceWhateverTheNumberOfSteps() {
        steps(new ControlPipeline(state, crawler, indexer, List.of(1, 2, 3, 4, 5), BOOK_BY_BOOK));

        assertEquals(1, state.reads);
    }

    private static List<NextStep> steps(ControlPipeline pipeline) {
        return Stream.generate(pipeline::runStep).takeWhile(report -> !report.idle()).map(StepReport::step).toList();
    }

    private static class RecordingIndexer implements Indexer {

        private final List<List<Integer>> batches = new ArrayList<>();

        @Override
        public Map<Integer, Outcome> index(List<Integer> bookIds) {
            batches.add(bookIds);
            Map<Integer, Outcome> outcomes = new LinkedHashMap<>();
            bookIds.forEach(bookId -> outcomes.put(bookId, bookId == MISSING_BOOK ? Outcome.failure("not found") : Outcome.success("indexed")));
            return outcomes;
        }
    }

    private static class InMemoryState implements ControlStateStore {

        private final Set<Integer> downloaded = new LinkedHashSet<>();
        private final Set<Integer> indexed = new LinkedHashSet<>();
        private int reads;

        @Override
        public Set<Integer> downloaded() {
            reads++;
            return new LinkedHashSet<>(downloaded);
        }

        @Override
        public Set<Integer> indexed() {
            return new LinkedHashSet<>(indexed);
        }

        @Override
        public void markDownloaded(int bookId) {
            downloaded.add(bookId);
        }

        @Override
        public void markIndexed(int bookId) {
            indexed.add(bookId);
        }
    }
}
