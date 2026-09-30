package org.ulpgc.tarantino.query.model;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Normalizes a query with the same rules as the indexer's Tokenizer, so query terms match indexed terms. */
public final class QueryTerms {

    private static final Pattern TERM = Pattern.compile("\\p{L}+");
    private static final int MIN_LENGTH = 2;

    private QueryTerms() {
    }

    public static Set<String> of(String query, Set<String> stopwords) {
        Set<String> terms = new LinkedHashSet<>();
        Matcher matcher = TERM.matcher(query.toLowerCase(Locale.ROOT));
        while (matcher.find()) {
            String term = matcher.group();
            if (term.length() >= MIN_LENGTH && !stopwords.contains(term)) {
                terms.add(term);
            }
        }
        return terms;
    }
}
