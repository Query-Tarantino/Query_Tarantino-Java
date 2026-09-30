package org.ulpgc.tarantino.query.adapters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ulpgc.tarantino.query.ports.InvertedIndexReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/** Reads {@code datamarts/inverted_index.json}; the whole file is loaded on first use. */
public class MonolithicJsonIndexReader implements InvertedIndexReader {

    private static final TypeReference<Map<String, Set<Integer>>> INDEX_TYPE = new TypeReference<>() {
    };

    private final Path file;
    private Map<String, Set<Integer>> index;

    public MonolithicJsonIndexReader(Path file) {
        this.file = file;
    }

    @Override
    public Set<Integer> postings(String term) {
        return index().getOrDefault(term, Set.of());
    }

    private Map<String, Set<Integer>> index() {
        if (index == null) {
            try {
                index = Files.exists(file) ? new ObjectMapper().readValue(file.toFile(), INDEX_TYPE) : Map.of();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        return index;
    }
}
