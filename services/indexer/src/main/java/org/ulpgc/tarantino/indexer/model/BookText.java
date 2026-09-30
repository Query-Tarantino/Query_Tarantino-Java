package org.ulpgc.tarantino.indexer.model;

import java.nio.file.Path;

public record BookText(int bookId, String header, String body, Path bodyPath) {
}
