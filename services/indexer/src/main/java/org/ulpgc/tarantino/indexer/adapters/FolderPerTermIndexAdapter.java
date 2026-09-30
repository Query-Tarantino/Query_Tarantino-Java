package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class FolderPerTermIndexAdapter implements InvertedIndexStorage {

    private final Path root;
    private final PendingPostings pending = new PendingPostings();

    public FolderPerTermIndexAdapter(Path root) {
        this.root = root;
    }

    @Override
    public void add(TermOccurrences occurrences) {
        pending.add(occurrences);
    }

    @Override
    public void flush() {
        pending.drain().forEach(this::merge);
    }

    private void merge(String term, Set<Integer> ids) {
        Path file = TermFiles.file(root, term);
        TreeSet<Integer> postings = storedPostings(file);
        if (postings.addAll(ids)) {
            writeAtomically(file, postings);
        }
    }

    private static TreeSet<Integer> storedPostings(Path file) {
        return lines(file).stream().map(Integer::valueOf).collect(Collectors.toCollection(TreeSet::new));
    }

    private static List<String> lines(Path file) {
        try {
            return Files.exists(file) ? Files.readAllLines(file) : List.of();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeAtomically(Path file, TreeSet<Integer> postings) {
        try {
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            Files.createDirectories(file.getParent());
            Files.writeString(temporary, content(postings));
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String content(TreeSet<Integer> postings) {
        return postings.stream().map(id -> id + "\n").collect(Collectors.joining());
    }
}
