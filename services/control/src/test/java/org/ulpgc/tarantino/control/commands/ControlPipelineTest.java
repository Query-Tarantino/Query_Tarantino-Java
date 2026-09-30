package org.ulpgc.tarantino.control.commands;

import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.control.model.NextStep;
import org.ulpgc.tarantino.control.ports.ControlStateStore;
import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;
import org.ulpgc.tarantino.crawler.model.DownloadException;
import org.ulpgc.tarantino.crawler.model.FailureReason;
import org.ulpgc.tarantino.crawler.model.StoredPaths;
import org.ulpgc.tarantino.indexer.commands.IndexBookCommand;
import org.ulpgc.tarantino.indexer.model.BookText;
import org.ulpgc.tarantino.indexer.model.HeaderParser;
import org.ulpgc.tarantino.indexer.model.Tokenizer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlPipelineTest {

    private static final String RAW = "Title: T\n*** START OF THE PROJECT GUTENBERG EBOOK T ***\nword\n*** END OF THE PROJECT GUTENBERG EBOOK T ***";

    private final InMemoryState state = new InMemoryState();
    private final Map<Integer, String> datalake = new HashMap<>();

    @Test
    void downloadsThenIndexesEachCandidateAndSkipsFailures() {
        ControlPipeline pipeline = pipeline(List.of(1, 404, 2));

        List<NextStep> steps = new ArrayList<>();
        NextStep step;
        do {
            step = pipeline.runStep();
            steps.add(step);
        } while (step.action() != NextStep.Action.IDLE);

        assertEquals(List.of(
                new NextStep(NextStep.Action.DOWNLOAD, 1),
                new NextStep(NextStep.Action.INDEX, 1),
                new NextStep(NextStep.Action.DOWNLOAD, 404),
                new NextStep(NextStep.Action.DOWNLOAD, 2),
                new NextStep(NextStep.Action.INDEX, 2),
                new NextStep(NextStep.Action.IDLE, 0)), steps);
        assertEquals(Set.of(1, 2), state.downloaded());
        assertEquals(Set.of(1, 2), state.indexed());
    }

    @Test
    void resumesByIndexingBooksDownloadedBeforeAnInterruption() {
        datalake.put(7, RAW);
        state.markDownloaded(7);

        assertEquals(new NextStep(NextStep.Action.INDEX, 7), pipeline(List.of()).next());
    }

    private ControlPipeline pipeline(List<Integer> candidates) {
        IngestBookCommand ingest = new IngestBookCommand(
                bookId -> {
                    if (bookId == 404) throw new DownloadException(FailureReason.NOT_FOUND, "missing");
                    return RAW;
                },
                new org.ulpgc.tarantino.crawler.ports.DatalakeStorage() {
                    public StoredPaths save(org.ulpgc.tarantino.crawler.model.BookText book) {
                        datalake.put(book.bookId(), RAW);
                        return locate(book.bookId()).orElseThrow();
                    }

                    public Optional<StoredPaths> locate(int bookId) {
                        return datalake.containsKey(bookId)
                                ? Optional.of(new StoredPaths(Path.of(bookId + ".header.txt"), Path.of(bookId + ".body.txt")))
                                : Optional.empty();
                    }
                });
        IndexBookCommand index = new IndexBookCommand(
                bookId -> Optional.ofNullable(datalake.get(bookId))
                        .map(raw -> new BookText(bookId, "Title: T", "word", Path.of(bookId + ".body.txt"))),
                new HeaderParser(),
                new Tokenizer(Set.of()),
                new org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage() {
                    public void add(org.ulpgc.tarantino.indexer.model.TermOccurrences occurrences) {
                    }

                    public void flush() {
                    }
                },
                book -> {
                });
        return new ControlPipeline(state, ingest, index, candidates);
    }

    private static class InMemoryState implements ControlStateStore {
        private final Set<Integer> downloaded = new LinkedHashSet<>();
        private final Set<Integer> indexed = new LinkedHashSet<>();

        public Set<Integer> downloaded() {
            return new LinkedHashSet<>(downloaded);
        }

        public Set<Integer> indexed() {
            return new LinkedHashSet<>(indexed);
        }

        public void markDownloaded(int bookId) {
            downloaded.add(bookId);
        }

        public void markIndexed(int bookId) {
            indexed.add(bookId);
        }
    }
}
