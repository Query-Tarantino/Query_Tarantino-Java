package org.ulpgc.tarantino.indexer.commands;

public record IndexResult(int bookId, boolean indexed, int uniqueTerms) {
}
