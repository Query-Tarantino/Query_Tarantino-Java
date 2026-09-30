package org.ulpgc.tarantino.crawler.adapters;

import org.ulpgc.tarantino.crawler.model.BookText;
import org.ulpgc.tarantino.crawler.model.StoredPaths;
import org.ulpgc.tarantino.crawler.ports.DatalakeStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.stream.Stream;

public class TimeBasedDatalakeAdapter implements DatalakeStorage {

    private static final DateTimeFormatter DAY_DIRECTORY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR_DIRECTORY = DateTimeFormatter.ofPattern("HH");
    private static final String HEADER_SUFFIX = ".header.txt";
    private static final String BODY_SUFFIX = ".body.txt";
    private static final int BOOK_FILE_DEPTH = 3;

    private final Path root;
    private final Clock clock;

    public TimeBasedDatalakeAdapter(Path root) {
        this(root, Clock.systemUTC());
    }

    public TimeBasedDatalakeAdapter(Path root, Clock clock) {
        this.root = root;
        this.clock = clock;
    }

    @Override
    public StoredPaths save(BookText book) {
        StoredPaths paths = pathsIn(currentDirectory(), book.bookId());
        write(paths, book);
        return paths;
    }

    @Override
    public Optional<StoredPaths> pathsOf(int bookId) {
        return bodyFile(bookId).map(body -> pathsIn(body.getParent(), bookId));
    }

    private Path currentDirectory() {
        LocalDateTime now = LocalDateTime.now(clock);
        return root.resolve(DAY_DIRECTORY.format(now)).resolve(HOUR_DIRECTORY.format(now));
    }

    private static StoredPaths pathsIn(Path directory, int bookId) {
        return new StoredPaths(directory.resolve(bookId + HEADER_SUFFIX), directory.resolve(bookId + BODY_SUFFIX));
    }

    private Optional<Path> bodyFile(int bookId) {
        return Files.isDirectory(root) ? firstFileNamed(bookId + BODY_SUFFIX) : Optional.empty();
    }

    private Optional<Path> firstFileNamed(String name) {
        try (Stream<Path> files = Files.find(root, BOOK_FILE_DEPTH, (path, attributes) -> path.endsWith(name))) {
            return files.findFirst();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void write(StoredPaths paths, BookText book) {
        try {
            writeAtomically(paths.header(), book.header());
            writeAtomically(paths.body(), book.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.createDirectories(target.getParent());
        Files.writeString(temporary, content);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
