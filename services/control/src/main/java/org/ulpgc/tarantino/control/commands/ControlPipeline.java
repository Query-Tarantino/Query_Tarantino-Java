package org.ulpgc.tarantino.control.commands;

import org.ulpgc.tarantino.control.model.NextStep;
import org.ulpgc.tarantino.control.ports.ControlStateStore;
import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;
import org.ulpgc.tarantino.indexer.commands.IndexBookCommand;

import java.util.List;

public class ControlPipeline {

    private final ControlStateStore state;
    private final IngestBookCommand ingest;
    private final IndexBookCommand index;
    private final List<Integer> candidates;

    public ControlPipeline(ControlStateStore state, IngestBookCommand ingest, IndexBookCommand index,
                           List<Integer> candidates) {
        this.state = state;
        this.ingest = ingest;
        this.index = index;
        this.candidates = candidates;
    }

    public NextStep next() {
        // TODO: INDEX if downloaded - indexed is not empty, else DOWNLOAD the first candidate not downloaded, else IDLE
        throw new UnsupportedOperationException("TODO");
    }

    public NextStep runStep() {
        // TODO: execute next() and update the control files only after the command succeeds
        throw new UnsupportedOperationException("TODO");
    }
}
