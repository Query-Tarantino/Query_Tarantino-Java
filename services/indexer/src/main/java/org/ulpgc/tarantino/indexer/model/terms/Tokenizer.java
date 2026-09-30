package org.ulpgc.tarantino.indexer.model.terms;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Tokenizer {

    private static final Pattern TERM = Pattern.compile("\\p{L}+");
    private static final int MIN_TERM_LENGTH = 2;

    private final Set<String> stopwords;

    public Tokenizer(Set<String> stopwords) {
        this.stopwords = stopwords;
    }

    public TermOccurrences occurrences(int bookId, String body) {
        return new TermOccurrences(bookId, frequencies(body));
    }

    private Map<String, Integer> frequencies(String body) {
        return terms(body).collect(Collectors.groupingBy(Function.identity(), Collectors.summingInt(term -> 1)));
    }

    private Stream<String> terms(String body) {
        return TERM.matcher(body.toLowerCase(Locale.ROOT)).results()
                .map(MatchResult::group)
                .filter(this::isIndexable);
    }

    private boolean isIndexable(String term) {
        return term.codePointCount(0, term.length()) >= MIN_TERM_LENGTH && !stopwords.contains(term);
    }
}
