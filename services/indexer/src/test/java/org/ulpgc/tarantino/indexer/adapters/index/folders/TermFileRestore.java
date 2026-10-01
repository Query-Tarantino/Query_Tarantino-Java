package org.ulpgc.tarantino.indexer.adapters.index.folders;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

/** Puts back the term files of some terms as they are in a snapshot, deleting the ones it does not have. */
public final class TermFileRestore {

    private TermFileRestore() {
    }

    public static void restore(Path snapshot, Path index, Collection<String> terms) {
        terms.forEach(term -> restore(TermFiles.file(snapshot, term), TermFiles.file(index, term)));
    }

    private static void restore(Path stored, Path current) {
        try {
            if (Files.exists(stored)) {
                Files.createDirectories(current.getParent());
                Files.copy(stored, current, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(current);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
