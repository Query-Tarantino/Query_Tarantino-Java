package org.ulpgc.tarantino.query;

import org.ulpgc.tarantino.query.adapters.FolderPerTermIndexReader;
import org.ulpgc.tarantino.query.adapters.MongodbIndexReader;
import org.ulpgc.tarantino.query.adapters.MongodbMetadataReader;
import org.ulpgc.tarantino.query.adapters.MonolithicJsonIndexReader;
import org.ulpgc.tarantino.query.adapters.SqliteMetadataReader;
import org.ulpgc.tarantino.query.commands.SearchCommand;
import org.ulpgc.tarantino.query.ports.InvertedIndexReader;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class QueryFactory {

    private QueryFactory() {
    }

    public static SearchCommand searchCommand(QueryConfig config) {
        return new SearchCommand(invertedIndex(config), metadata(config), stopwords(config));
    }

    public static InvertedIndexReader invertedIndex(QueryConfig config) {
        return switch (config.index()) {
            case "json" -> new MonolithicJsonIndexReader(config.datamarts().resolve("inverted_index.json"));
            case "folders" -> new FolderPerTermIndexReader(config.datamarts().resolve("inverted_index"));
            case "mongo" -> new MongodbIndexReader(config.mongoUri());
            default -> throw new IllegalArgumentException("Unknown index structure: " + config.index());
        };
    }

    public static MetadataReader metadata(QueryConfig config) {
        return switch (config.metadata()) {
            case "sqlite" -> new SqliteMetadataReader(config.datamarts().resolve("metadata.db"));
            case "mongo" -> new MongodbMetadataReader(config.mongoUri());
            default -> throw new IllegalArgumentException("Unknown metadata backend: " + config.metadata());
        };
    }

    private static Set<String> stopwords(QueryConfig config) {
        try {
            return Files.readAllLines(config.workload().resolve("stopwords.txt")).stream()
                    .map(line -> line.strip().toLowerCase(Locale.ROOT))
                    .filter(line -> !line.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
