package org.ulpgc.tarantino.crawler.adapters.datalake;

import org.ulpgc.tarantino.crawler.model.book.BookText;
import org.ulpgc.tarantino.crawler.model.book.StoredPaths;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public final class BookFiles {

    private BookFiles() {
    }

    public static StoredPaths write(StoredPaths paths, BookText book) {
        try {
            writeAtomically(paths.header(), book.header());
            writeAtomically(paths.body(), book.body());
            return paths;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Optional<StoredPaths> existing(StoredPaths paths) {
        return Optional.of(paths).filter(candidate -> Files.exists(candidate.body()));
    }

    public static List<Path> children(Path directory) {
        if (!Files.isDirectory(directory)) {
            return List.of();
        }
        try (Stream<Path> children = Files.list(directory)) {
            return children.toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static boolean modifiedSince(Path file, Instant instant) {
        try {
            return Files.exists(file) && !Files.getLastModifiedTime(file).toInstant().isBefore(instant);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static int bookId(Path file, String suffix) {
        String name = file.getFileName().toString();
        return Integer.parseInt(name.substring(0, name.length() - suffix.length()));
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.createDirectories(target.getParent());
        Files.writeString(temporary, content);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
