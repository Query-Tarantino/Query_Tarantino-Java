package org.ulpgc.tarantino.indexer.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HeaderParser {

    private static final Pattern TITLE = field("Title");
    private static final Pattern AUTHOR = field("Author");
    private static final Pattern LANGUAGE = field("Language");

    /** Missing fields are returned as {@code null}. */
    public Book parse(BookText text) {
        String header = text.header();
        return new Book(text.bookId(), find(TITLE, header), find(AUTHOR, header), find(LANGUAGE, header), text.bodyPath());
    }

    private static Pattern field(String name) {
        return Pattern.compile("^" + name + ":[ \\t]*(.+)$", Pattern.MULTILINE);
    }

    private static String find(Pattern pattern, String header) {
        Matcher matcher = pattern.matcher(header);
        return matcher.find() ? matcher.group(1).strip() : null;
    }
}
