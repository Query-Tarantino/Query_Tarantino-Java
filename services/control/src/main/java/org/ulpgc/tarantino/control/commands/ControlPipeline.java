package org.ulpgc.tarantino.control.commands;

import org.ulpgc.tarantino.control.model.NextStep;
import org.ulpgc.tarantino.control.ports.ControlStateStore;
import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;
import org.ulpgc.tarantino.crawler.commands.IngestResult;
import org.ulpgc.tarantino.indexer.commands.IndexBookCommand;
import org.ulpgc.tarantino.indexer.commands.IndexResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Decides and runs one step at a time: index pending books first, otherwise download the next candidate.
 * Control files are updated only after a command succeeds, so an interrupted run resumes without losing work.
 */
public class ControlPipeline {

    private final ControlStateStore state;
    private final IngestBookCommand ingest;
    private final IndexBookCommand index;
    private final List<Integer> candidates;
    private final Set<Integer> failed = new HashSet<>();

    public ControlPipeline(ControlStateStore state, IngestBookCommand ingest, IndexBookCommand index,
                           List<Integer> candidates) {
        this.state = state;
        this.ingest = ingest;
        this.index = index;
        this.candidates = candidates;
    }

    public NextStep next() {
        Set<Integer> downloaded = state.downloaded();
        Set<Integer> indexed = state.indexed();
        for (int bookId : downloaded) {
            if (!indexed.contains(bookId) && !failed.contains(bookId)) {
                return new NextStep(NextStep.Action.INDEX, bookId);
            }
        }
        for (int bookId : candidates) {
            if (!downloaded.contains(bookId) && !failed.contains(bookId)) {
                return new NextStep(NextStep.Action.DOWNLOAD, bookId);
            }
        }
        return new NextStep(NextStep.Action.IDLE, 0);
    }

    public NextStep runStep() {
        NextStep step = next();
        switch (step.action()) {
            case INDEX -> {
                IndexResult result = index.execute(step.bookId());
                if (result.indexed()) {
                    state.markIndexed(step.bookId());
                    log("Book %d indexed (%d unique terms)", step.bookId(), result.uniqueTerms());
                } else {
                    failed.add(step.bookId());
                    log("Book %d could not be indexed: not found in the datalake", step.bookId());
                }
            }
            case DOWNLOAD -> {
                IngestResult result = ingest.execute(step.bookId());
                if (result.succeeded()) {
                    state.markDownloaded(step.bookId());
                    log("Book %d downloaded to %s", step.bookId(), result.paths().body().getParent());
                } else {
                    failed.add(step.bookId());
                    log("Book %d skipped: %s", step.bookId(), result.failure());
                }
            }
            case IDLE -> log("Nothing left to do");
        }
        return step;
    }

    private static void log(String format, Object... arguments) {
        System.out.println("[CONTROL] " + format.formatted(arguments));
    }
}
