package org.ulpgc.tarantino.indexer.adapters;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mongodb.MongoDBContainer;
import org.ulpgc.tarantino.indexer.model.Book;
import org.ulpgc.tarantino.indexer.model.TermOccurrences;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers(disabledWithoutDocker = true)
class MongodbAdaptersTest {

    @Container
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @AfterEach
    void dropDatabase() {
        try (MongoClient client = MongoClients.create(MONGO.getConnectionString())) {
            client.getDatabase("tarantino").drop();
        }
    }

    @Test
    void indexAddsUniquePostingsPerTerm() {
        MongodbIndexAdapter index = new MongodbIndexAdapter(MONGO.getConnectionString());
        index.add(new TermOccurrences(1342, Map.of("island", 2, "whale", 1)));
        index.add(new TermOccurrences(5, Map.of("island", 1)));
        index.flush();
        index.add(new TermOccurrences(5, Map.of("island", 1)));
        index.flush();

        assertEquals(List.of(5, 1342), postings("island"));
        assertEquals(List.of(1342), postings("whale"));
    }

    @Test
    void flushWithoutPendingTermsWritesNothing() {
        new MongodbIndexAdapter(MONGO.getConnectionString()).flush();

        assertEquals(0, database().getCollection("inverted_index").countDocuments());
    }

    @Test
    void metadataIsUpsertedByBookId() {
        MongodbMetadataAdapter metadata = new MongodbMetadataAdapter(MONGO.getConnectionString());
        metadata.save(new Book(5, "Old title", null, "English", Path.of("datalake", "5", "body.txt")));
        metadata.save(new Book(5, "Robinson Crusoe", null, "English", Path.of("datalake", "5", "body.txt")));

        Document book = Objects.requireNonNull(database().getCollection("books").find(Filters.eq("book_id", 5)).first());
        assertEquals(1, database().getCollection("books").countDocuments());
        assertEquals("Robinson Crusoe", book.getString("title"));
        assertNull(book.getString("author"));
        assertEquals("datalake/5/body.txt", book.getString("path"));
    }

    private List<Integer> postings(String term) {
        Document document = Objects.requireNonNull(database().getCollection("inverted_index").find(Filters.eq("term", term)).first());
        return document.getList("postings", Integer.class).stream().sorted().toList();
    }

    private static MongoDatabase database() {
        return MongoDatabases.database(MONGO.getConnectionString());
    }
}
