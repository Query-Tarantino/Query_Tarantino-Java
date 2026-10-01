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
import java.util.Optional;
import java.util.Set;

/**
 * Reads the state once and keeps it in memory, so each step costs the same however many books are done.
 * Downloaded and failed ids only grow during a run, so the first pending candidate never moves back and the
 * books to index form a queue in downloaded order.
 */
public class ControlPipeline {

    private final ControlStateStore state;
    private final Crawler crawler;
    private final Indexer indexer;
    private final List<Integer> candidates;
    private final Set<Integer> downloaded;
    private final Deque<Integer> toIndex = new ArrayDeque<>();
    private final Set<Integer> failed = new HashSet<>();
    private int nextCandidate;

    public ControlPipeline(ControlStateStore state, Crawler crawler, Indexer indexer, List<Integer> candidates) {
        this.state = state;
        this.crawler = crawler;
        this.indexer = indexer;
        this.candidates = candidates;
        this.downloaded = new LinkedHashSet<>(state.downloaded());
        Set<Integer> indexed = state.indexed();
        downloaded.stream().filter(id -> !indexed.contains(id)).forEach(toIndex::addLast);
    }

    public NextStep nextStep() {
        return Optional.ofNullable(toIndex.peekFirst()).map(NextStep::index)
                .or(() -> pendingCandidate().map(NextStep::download))
                .orElseGet(NextStep::idle);
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
            case INDEX -> indexed(step.bookId(), indexer.index(step.bookId()));
            case DOWNLOAD -> downloaded(step.bookId(), crawler.ingest(step.bookId()));
            case IDLE -> Outcome.success("nothing left to do");
        };
    }

    private Outcome indexed(int bookId, Outcome outcome) {
        toIndex.removeFirst();
        if (outcome.succeeded()) {
            state.markIndexed(bookId);
        } else {
            failed.add(bookId);
        }
        return outcome;
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
