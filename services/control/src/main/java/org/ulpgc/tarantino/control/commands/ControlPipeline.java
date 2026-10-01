package org.ulpgc.tarantino.control.commands;

import org.ulpgc.tarantino.control.model.NextStep;
import org.ulpgc.tarantino.control.model.Outcome;
import org.ulpgc.tarantino.control.model.StepReport;
import org.ulpgc.tarantino.control.ports.ControlStateStore;
import org.ulpgc.tarantino.control.ports.Crawler;
import org.ulpgc.tarantino.control.ports.Indexer;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Reads the state once and keeps it in memory, so each step costs the same however many books are done.
 * Downloaded and failed ids only grow during a run, so the first pending candidate never moves back and the
 * books to index form a queue in downloaded order. Books are indexed in batches, one index flush per batch,
 * and marked as indexed only after it (SPEC §9).
 */
public class ControlPipeline {

    private static final Outcome MISSING_OUTCOME = Outcome.failure("no outcome from the indexer");

    private final ControlStateStore state;
    private final Crawler crawler;
    private final Indexer indexer;
    private final List<Integer> candidates;
    private final int indexBatch;
    private final Set<Integer> downloaded;
    private final Deque<Integer> toIndex = new ArrayDeque<>();
    private final Set<Integer> failed = new HashSet<>();
    private int nextCandidate;

    public ControlPipeline(ControlStateStore state, Crawler crawler, Indexer indexer, List<Integer> candidates,
                           int indexBatch) {
        this.state = state;
        this.crawler = crawler;
        this.indexer = indexer;
        this.candidates = candidates;
        this.indexBatch = indexBatch;
        this.downloaded = new LinkedHashSet<>(state.downloaded());
        Set<Integer> indexed = state.indexed();
        downloaded.stream().filter(id -> !indexed.contains(id)).forEach(toIndex::addLast);
    }

    public NextStep nextStep() {
        if (toIndex.size() >= indexBatch || (!toIndex.isEmpty() && pendingCandidate().isEmpty())) {
            return NextStep.index(toIndex.stream().limit(indexBatch).toList());
        }
        return pendingCandidate().map(NextStep::download).orElseGet(NextStep::idle);
    }

    public StepReport runStep() {
        NextStep step = nextStep();
        return new StepReport(step, outcome(step));
    }

    private Optional<Integer> pendingCandidate() {
        while (nextCandidate < candidates.size() && isDone(candidates.get(nextCandidate))) {
            nextCandidate++;
        }
        return nextCandidate < candidates.size() ? Optional.of(candidates.get(nextCandidate)) : Optional.empty();
    }

    private boolean isDone(int bookId) {
        return downloaded.contains(bookId) || failed.contains(bookId);
    }

    private Outcome outcome(NextStep step) {
        return switch (step.action()) {
            case INDEX -> indexed(step.bookIds(), indexer.index(step.bookIds()));
            case DOWNLOAD -> downloaded(step.bookIds().getFirst(), crawler.ingest(step.bookIds().getFirst()));
            case IDLE -> Outcome.success("nothing left to do");
        };
    }

    private Outcome indexed(List<Integer> bookIds, Map<Integer, Outcome> outcomes) {
        for (int bookId : bookIds) {
            toIndex.removeFirst();
            if (outcomeOf(bookId, outcomes).succeeded()) {
                state.markIndexed(bookId);
            } else {
                failed.add(bookId);
            }
        }
        return bookIds.size() == 1 ? outcomeOf(bookIds.getFirst(), outcomes) : summary(bookIds, outcomes);
    }

    private static Outcome summary(List<Integer> bookIds, Map<Integer, Outcome> outcomes) {
        long indexed = bookIds.stream().filter(bookId -> outcomeOf(bookId, outcomes).succeeded()).count();
        return indexed == bookIds.size()
                ? Outcome.success(indexed + " indexed")
                : Outcome.failure(indexed + " indexed, " + (bookIds.size() - indexed) + " skipped");
    }

    private static Outcome outcomeOf(int bookId, Map<Integer, Outcome> outcomes) {
        return outcomes.getOrDefault(bookId, MISSING_OUTCOME);
    }

    private Outcome downloaded(int bookId, Outcome outcome) {
        if (outcome.succeeded()) {
            state.markDownloaded(bookId);
            downloaded.add(bookId);
            toIndex.addLast(bookId);
        } else {
            failed.add(bookId);
        }
        return outcome;
    }
}
