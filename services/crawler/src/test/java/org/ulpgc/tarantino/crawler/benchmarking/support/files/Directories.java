package org.ulpgc.tarantino.crawler.benchmarking.support.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class Directories {

    private Directories() {
    }

    public static void delete(Path root) {
        entries(root).stream().sorted(Comparator.reverseOrder()).forEach(Directories::deleteEntry);
    }

    public static long fileCount(Path root) {
        return fileCount(root, file -> true);
    }

    public static long fileCount(Path root, Predicate<Path> filter) {
        return entries(root).stream().filter(Files::isRegularFile).filter(filter).count();
    }

    public static long directoryCount(Path root) {
        return entries(root).stream().filter(Files::isDirectory).filter(Predicate.not(root::equals)).count();
    }

    public static long diskUsage(Path root) {
        return entries(root).stream().filter(Files::isRegularFile).mapToLong(Directories::size).sum();
    }

    private static List<Path> entries(Path root) {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (Stream<Path> entries = Files.walk(root)) {
            return entries.toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteEntry(Path entry) {
        try {
            Files.delete(entry);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long size(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
