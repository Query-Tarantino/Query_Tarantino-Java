package org.ulpgc.tarantino.indexer.adapters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ulpgc.tarantino.indexer.model.TermOccurrences;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Layout: {@code datamarts/inverted_index.json} as {@code {"term": [bookId, ...]}}, terms and ids sorted.
 * The whole index is loaded on first use and rewritten on every flush.
 */
public class MonolithicJsonIndexAdapter implements InvertedIndexStorage {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<TreeMap<String, TreeSet<Integer>>> INDEX_TYPE = new TypeReference<>() {
    };

    private final Path file;
    private SortedMap<String, SortedSet<Integer>> index;

    public MonolithicJsonIndexAdapter(Path file) {
        this.file = file;
    }

    @Override
    public void add(TermOccurrences occurrences) {
        SortedMap<String, SortedSet<Integer>> postings = index();
        for (String term : occurrences.frequencies().keySet()) {
            postings.computeIfAbsent(term, key -> new TreeSet<>()).add(occurrences.bookId());
        }
    }

    @Override
    public void flush() {
        try {
            Files.createDirectories(file.toAbsolutePath().getParent());
            Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
            MAPPER.writeValue(temporary.toFile(), index());
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private SortedMap<String, SortedSet<Integer>> index() {
        if (index == null) {
            index = new TreeMap<>();
            if (Files.exists(file)) {
                try {
                    index.putAll(MAPPER.readValue(file.toFile(), INDEX_TYPE));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
        return index;
    }
}
