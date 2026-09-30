package org.ulpgc.tarantino.crawler.model;

public final class GutenbergText {

    public static final String START_MARKER = "*** START OF THE PROJECT GUTENBERG EBOOK";
    public static final String END_MARKER = "*** END OF THE PROJECT GUTENBERG EBOOK";

    private GutenbergText() {
    }

    public static BookText split(int bookId, String rawText) {
        // TODO: split on START_MARKER / END_MARKER, discard footer, throw MISSING_MARKERS if absent
        throw new UnsupportedOperationException("TODO");
    }
}
