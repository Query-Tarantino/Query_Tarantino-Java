package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.ports.StopwordsLoader;

import java.nio.file.Path;
import java.util.Set;

public class FileStopwordsLoader implements StopwordsLoader {

    private final Path file;

    public FileStopwordsLoader(Path file) {
        this.file = file;
    }

    @Override
    public Set<String> load() {
        throw new UnsupportedOperationException("TODO");
    }
}
