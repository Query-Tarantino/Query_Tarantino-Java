package org.ulpgc.tarantino.indexer.adapters.index.folders;

import java.nio.file.Path;

final class TermFiles {

    private static final String SUFFIX = ".txt";

    private TermFiles() {
    }

    static Path file(Path root, String term) {
        return root.resolve(firstCharacter(term)).resolve(term + SUFFIX);
    }

    private static String firstCharacter(String term) {
        return term.substring(0, Character.charCount(term.codePointAt(0)));
    }
}
