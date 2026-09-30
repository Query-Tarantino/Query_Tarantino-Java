package org.ulpgc.tarantino.crawler.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GutenbergText {

    private static final Pattern START_MARKER =
            Pattern.compile("\\*\\*\\* ?START OF (THE|THIS) PROJECT GUTENBERG EBOOK[^\\n]*\\n");
    private static final Pattern END_MARKER =
            Pattern.compile("\\*\\*\\* ?END OF (THE|THIS) PROJECT GUTENBERG EBOOK");

    private GutenbergText() {
    }

    /** Splits a raw Gutenberg text into header and body; the marker lines and the footer are discarded. */
    public static BookText split(int bookId, String rawText) {
        String text = rawText.replace("\r\n", "\n");
        Matcher start = START_MARKER.matcher(text);
        if (!start.find()) {
            throw new DownloadException(FailureReason.MISSING_MARKERS, "Start marker not found in book " + bookId);
        }
        Matcher end = END_MARKER.matcher(text);
        if (!end.find(start.end())) {
            throw new DownloadException(FailureReason.MISSING_MARKERS, "End marker not found in book " + bookId);
        }
        String header = text.substring(0, start.start()).strip();
        String body = text.substring(start.end(), end.start()).strip();
        return new BookText(bookId, header, body);
    }
}
