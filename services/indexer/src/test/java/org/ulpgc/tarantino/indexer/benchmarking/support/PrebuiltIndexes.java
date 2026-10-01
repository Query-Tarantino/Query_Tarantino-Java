package org.ulpgc.tarantino.indexer.benchmarking.support;

import org.ulpgc.tarantino.crawler.benchmarking.support.environment.BenchmarkPaths;
import org.ulpgc.tarantino.crawler.benchmarking.support.files.Directories;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Indexes of the first N books, built once per benchmark run and shared by every process of the benchmarks
 * that only need them as a starting point. Their content is deterministic, so reusing them changes no result.
 */
public final class PrebuiltIndexes {

    private static final String PREFIX = "prebuilt-";
    private static final String BUILT_MARKER = ".built";

    private PrebuiltIndexes() {
    }

    public static BenchmarkStore of(String index, int books, IndexFixture fixture) {
        BenchmarkStore store = store(index, books);
        Path marker = marker(index, books);
        if (!Files.exists(marker)) {
            store.clear();
            fixture.index(store.invertedIndex(), fixture.dataset().ids(books));
            create(marker);
        }
        return store;
    }

    public static Path file(String name) {
        return BenchmarkPaths.scratch(PREFIX + name);
    }

    public static void deleteAll() {
        prebuiltEntries().filter(entry -> entry.getFileName().toString().endsWith(BUILT_MARKER))
                .forEach(PrebuiltIndexes::deleteStoreOf);
        prebuiltEntries().forEach(Directories::delete);
    }

    private static BenchmarkStore store(String index, int books) {
        return BenchmarkStore.forIndex(index, PREFIX + "index-" + index + "-" + books);
    }

    private static Path marker(String index, int books) {
        return file(index + "-" + books + BUILT_MARKER);
    }

    private static void deleteStoreOf(Path marker) {
        String name = marker.getFileName().toString();
        String[] indexAndBooks = name.substring(PREFIX.length(), name.length() - BUILT_MARKER.length()).split("-");
        store(indexAndBooks[0], Integer.parseInt(indexAndBooks[1])).clear();
    }

    private static Stream<Path> prebuiltEntries() {
        Path root = BenchmarkPaths.scratchRoot();
        if (!Files.isDirectory(root)) {
            return Stream.empty();
        }
        try (Stream<Path> entries = Files.list(root)) {
            List<Path> prebuilt = entries.filter(entry -> entry.getFileName().toString().startsWith(PREFIX)).toList();
            return prebuilt.stream();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void create(Path marker) {
        try {
            Files.createDirectories(marker.getParent());
            Files.createFile(marker);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
