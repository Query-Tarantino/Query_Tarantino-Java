package org.ulpgc.tarantino.control.commands;

import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.control.model.NextStep;
import org.ulpgc.tarantino.control.model.Outcome;
import org.ulpgc.tarantino.control.model.StepReport;
import org.ulpgc.tarantino.control.ports.ControlStateStore;
import org.ulpgc.tarantino.control.ports.Crawler;
import org.ulpgc.tarantino.control.ports.Indexer;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlPipelineTest {

    private static final int MISSING_BOOK = 404;

    private final InMemoryState state = new InMemoryState();
    private final Crawler crawler = bookId -> bookId == MISSING_BOOK ? Outcome.failure("not found") : Outcome.success("stored");
    private final Indexer indexer = bookId -> Outcome.success("indexed");

    @Test
    void downloadsThenIndexesEachCandidateAndSkipsFailures() {
        ControlPipeline pipeline = new ControlPipeline(state, crawler, indexer, List.of(1, MISSING_BOOK, 2));

        List<NextStep> steps = Stream.generate(pipeline::runStep)
                .takeWhile(report -> !report.idle())
                .map(StepReport::step)
                .toList();

        assertEquals(List.of(NextStep.download(1), NextStep.index(1), NextStep.download(MISSING_BOOK),
                NextStep.download(2), NextStep.index(2)), steps);
        assertEquals(Set.of(1, 2), state.indexed());
    }

    @Test
    void resumesByIndexingBooksDownloadedBeforeAnInterruption() {
        state.markDownloaded(7);

        assertEquals(NextStep.index(7), new ControlPipeline(state, crawler, indexer, List.of()).nextStep());
    }

    private static class InMemoryState implements ControlStateStore {

        private final Set<Integer> downloaded = new LinkedHashSet<>();
        private final Set<Integer> indexed = new LinkedHashSet<>();

        @Override
        public Set<Integer> downloaded() {
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
