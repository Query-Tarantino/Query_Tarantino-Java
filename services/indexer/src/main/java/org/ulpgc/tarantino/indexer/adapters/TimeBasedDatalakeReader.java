package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.BookText;
import org.ulpgc.tarantino.indexer.ports.DatalakeReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

/** Layout: {@code datalake/YYYYMMDD/HH/<id>.header.txt + <id>.body.txt} */
public class TimeBasedDatalakeReader implements DatalakeReader {

    private final Path root;

    public TimeBasedDatalakeReader(Path root) {
        this.root = root;
    }

    @Override
    public Optional<BookText> read(int bookId) {
        if (!Files.isDirectory(root)) {
            return Optional.empty();
        }
        String bodyName = bookId + ".body.txt";
        try (Stream<Path> files = Files.find(root, 3, (path, attributes) -> path.getFileName().toString().equals(bodyName))) {
            Optional<Path> body = files.findFirst();
            if (body.isEmpty()) {
                return Optional.empty();
            }
            Path header = body.get().resolveSibling(bookId + ".header.txt");
            return Optional.of(new BookText(bookId, Files.readString(header), Files.readString(body.get()), body.get()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
