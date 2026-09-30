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

/** Layout: {@code datalake/YYYYMMDD/HH/<id>.header.txt + <id>.body.txt}, hour of download in UTC. */
public class TimeBasedDatalakeAdapter implements DatalakeStorage {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter HOUR = DateTimeFormatter.ofPattern("HH");

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
        LocalDateTime now = LocalDateTime.now(clock);
        Path directory = root.resolve(DAY.format(now)).resolve(HOUR.format(now));
        StoredPaths paths = new StoredPaths(
                directory.resolve(book.bookId() + ".header.txt"),
                directory.resolve(book.bookId() + ".body.txt"));
        try {
            Files.createDirectories(directory);
            // The body is written last: its presence marks the book as completely stored.
            writeAtomically(paths.header(), book.header());
            writeAtomically(paths.body(), book.body());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return paths;
    }

    @Override
    public Optional<StoredPaths> locate(int bookId) {
        if (!Files.isDirectory(root)) {
            return Optional.empty();
        }
        String bodyName = bookId + ".body.txt";
        try (Stream<Path> files = Files.find(root, 3, (path, attributes) -> path.getFileName().toString().equals(bodyName))) {
            return files.findFirst()
                    .map(body -> new StoredPaths(body.resolveSibling(bookId + ".header.txt"), body));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeAtomically(Path target, String content) throws IOException {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, content);
        Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
