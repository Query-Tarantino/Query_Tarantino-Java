package org.ulpgc.tarantino.indexer.conformance;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.ulpgc.tarantino.indexer.model.book.Book;
import org.ulpgc.tarantino.indexer.model.book.BookText;
import org.ulpgc.tarantino.indexer.model.book.HeaderParser;
import org.ulpgc.tarantino.indexer.model.terms.Tokenizer;

import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.ulpgc.tarantino.crawler.conformance.ConformanceCases.cases;
import static org.ulpgc.tarantino.crawler.conformance.ConformanceCases.file;
import static org.ulpgc.tarantino.crawler.conformance.ConformanceCases.stopwords;
import static org.ulpgc.tarantino.crawler.conformance.ConformanceCases.textOrNull;

class IndexerConformanceTest {

    @TestFactory
    Stream<DynamicTest> extractsHeaderFields() {
        HeaderParser parser = new HeaderParser();
        return cases(file("header_fields.json")).stream().map(testCase -> DynamicTest.dynamicTest(testCase.get("name").asText(), () -> {
            Book book = parser.book(new BookText(1, testCase.get("header").asText(), "", Path.of("1.body.txt")));
            assertEquals(textOrNull(testCase.get("title")), book.title());
            assertEquals(textOrNull(testCase.get("author")), book.author());
            assertEquals(textOrNull(testCase.get("language")), book.language());
        }));
    }

    @TestFactory
    Stream<DynamicTest> countsTermsOfABody() {
        JsonNode terms = file("terms.json");
        Tokenizer tokenizer = new Tokenizer(stopwords(terms));
        return cases(terms).stream().map(testCase -> DynamicTest.dynamicTest(testCase.get("name").asText(), () -> {
            Map<String, Integer> counted = tokenizer.occurrences(1, testCase.get("text").asText()).frequencies();
            assertEquals(frequencies(testCase.get("terms")), new TreeMap<>(counted));
        }));
    }

    private static Map<String, Integer> frequencies(JsonNode terms) {
        Map<String, Integer> frequencies = new TreeMap<>();
        terms.properties().forEach(term -> frequencies.put(term.getKey(), term.getValue().asInt()));
        return frequencies;
    }
}
