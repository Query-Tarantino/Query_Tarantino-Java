package org.ulpgc.tarantino.indexer.model;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Terms are maximal runs of Unicode letters, lowercased, at least {@value #MIN_LENGTH} characters long
 * and not in the stopword list. These rules must match the Python and C# implementations exactly.
 */
public class Tokenizer {

    private static final Pattern TERM = Pattern.compile("\\p{L}+");
    private static final int MIN_LENGTH = 2;

    private final Set<String> stopwords;

    public Tokenizer(Set<String> stopwords) {
        this.stopwords = stopwords;
    }

    public TermOccurrences tokenize(int bookId, String body) {
        Map<String, Integer> frequencies = new HashMap<>();
        Matcher matcher = TERM.matcher(body.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String term = matcher.group();
            if (term.length() >= MIN_LENGTH && !stopwords.contains(term)) {
                frequencies.merge(term, 1, Integer::sum);
            }
        }
        return new TermOccurrences(bookId, frequencies);
    }
}
