package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.ports.StopwordsLoader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class FileStopwordsLoader implements StopwordsLoader {

    private final Path file;

    public FileStopwordsLoader(Path file) {
        this.file = file;
    }

    @Override
    public Set<String> load() {
        try {
            return Files.readAllLines(file).stream()
                    .map(line -> line.strip().toLowerCase(Locale.ROOT))
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
