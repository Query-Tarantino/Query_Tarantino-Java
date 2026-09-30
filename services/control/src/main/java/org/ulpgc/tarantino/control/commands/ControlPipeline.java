package org.ulpgc.tarantino.control.commands;

import org.ulpgc.tarantino.control.model.NextStep;
import org.ulpgc.tarantino.control.model.Outcome;
import org.ulpgc.tarantino.control.model.StepReport;
import org.ulpgc.tarantino.control.ports.ControlStateStore;
import org.ulpgc.tarantino.control.ports.Crawler;
import org.ulpgc.tarantino.control.ports.Indexer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;

public class ControlPipeline {

    private final ControlStateStore state;
    private final Crawler crawler;
    private final Indexer indexer;
    private final List<Integer> candidates;
    private final Set<Integer> failed = new HashSet<>();

    public ControlPipeline(ControlStateStore state, Crawler crawler, Indexer indexer, List<Integer> candidates) {
        this.state = state;
        this.crawler = crawler;
        this.indexer = indexer;
        this.candidates = candidates;
    }

    public NextStep nextStep() {
        Set<Integer> downloaded = state.downloaded();
        return firstPending(downloaded, state.indexed()).map(NextStep::index)
                .or(() -> firstPending(candidates, downloaded).map(NextStep::download))
                .orElseGet(NextStep::idle);
    }

    public StepReport runStep() {
        NextStep step = nextStep();
        return new StepReport(step, outcome(step));
    }

    private Optional<Integer> firstPending(Collection<Integer> ids, Set<Integer> done) {
        return ids.stream()
                .filter(id -> !done.contains(id) && !failed.contains(id))
                .findFirst();
    }

    private Outcome outcome(NextStep step) {
        return switch (step.action()) {
            case INDEX -> register(step, indexer.index(step.bookId()), state::markIndexed);
            case DOWNLOAD -> register(step, crawler.ingest(step.bookId()), state::markDownloaded);
            case IDLE -> Outcome.success("nothing left to do");
        };
    }

    private Outcome register(NextStep step, Outcome outcome, IntConsumer markDone) {
        if (outcome.succeeded()) {
            markDone.accept(step.bookId());
        } else {
            failed.add(step.bookId());
        }
        return outcome;
    }
}
