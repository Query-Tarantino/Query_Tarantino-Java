package org.ulpgc.tarantino.crawler.benchmarking.support.files;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
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

    public static void copy(Path source, Path target) {
        entries(source).forEach(entry -> copyEntry(entry, target.resolve(source.relativize(entry))));
    }

    public static long fileCount(Path root) {
        return fileCount(root, file -> true);
    }

    public static long fileCount(Path root, Predicate<Path> filter) {
        return entries(root).stream().filter(Files::isRegularFile).filter(filter).count();
    }

    /** Measures the tree in one walk, reading sizes from the attributes the walk already has. */
    public static Footprint footprint(Path root) {
        if (!Files.exists(root)) {
            return Footprint.NONE;
        }
        try {
            long blockSize = Files.getFileStore(root).getBlockSize();
            FootprintVisitor visitor = new FootprintVisitor(root, blockSize);
            Files.walkFileTree(root, visitor);
            return visitor.footprint();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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

    private static void copyEntry(Path entry, Path target) {
        try {
            if (Files.isDirectory(entry)) {
                Files.createDirectories(target);
            } else {
                Files.copy(entry, target);
            }
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

    private static final class FootprintVisitor extends SimpleFileVisitor<Path> {

        private final Path root;
        private final long blockSize;
        private long files;
        private long directories;
        private long bytes;
        private long allocatedBytes;

        private FootprintVisitor(Path root, long blockSize) {
            this.root = root;
            this.blockSize = blockSize;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
            if (!directory.equals(root)) {
                directories++;
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
            if (attributes.isRegularFile()) {
                files++;
                bytes += attributes.size();
                allocatedBytes += (attributes.size() + blockSize - 1) / blockSize * blockSize;
            }
            return FileVisitResult.CONTINUE;
        }

        private Footprint footprint() {
            return new Footprint(files, directories, bytes, allocatedBytes);
        }
    }
}
