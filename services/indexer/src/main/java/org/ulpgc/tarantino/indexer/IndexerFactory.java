package org.ulpgc.tarantino.indexer;

import org.ulpgc.tarantino.indexer.adapters.BatchBasedDatalakeReader;
import org.ulpgc.tarantino.indexer.adapters.BookBasedDatalakeReader;
import org.ulpgc.tarantino.indexer.adapters.FileStopwordsLoader;
import org.ulpgc.tarantino.indexer.adapters.FolderPerTermIndexAdapter;
import org.ulpgc.tarantino.indexer.adapters.MongodbIndexAdapter;
import org.ulpgc.tarantino.indexer.adapters.MongodbMetadataAdapter;
import org.ulpgc.tarantino.indexer.adapters.MonolithicJsonIndexAdapter;
import org.ulpgc.tarantino.indexer.adapters.SqliteMetadataAdapter;
import org.ulpgc.tarantino.indexer.adapters.TimeBasedDatalakeReader;
import org.ulpgc.tarantino.indexer.commands.IndexBookCommand;
import org.ulpgc.tarantino.indexer.model.HeaderParser;
import org.ulpgc.tarantino.indexer.model.Tokenizer;
import org.ulpgc.tarantino.indexer.ports.DatalakeReader;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

public final class IndexerFactory {

    private IndexerFactory() {
    }

    public static IndexBookCommand indexCommand(IndexerConfig config) {
        Tokenizer tokenizer = new Tokenizer(new FileStopwordsLoader(config.workload().resolve("stopwords.txt")).load());
        return new IndexBookCommand(datalakeReader(config), new HeaderParser(), tokenizer,
                invertedIndex(config), metadata(config));
    }

    public static DatalakeReader datalakeReader(IndexerConfig config) {
        return switch (config.datalakeLayout()) {
            case "time" -> new TimeBasedDatalakeReader(config.datalake());
            case "book" -> new BookBasedDatalakeReader(config.datalake());
            case "batch" -> new BatchBasedDatalakeReader(config.datalake());
            default -> throw new IllegalArgumentException("Unknown datalake layout: " + config.datalakeLayout());
        };
    }

    public static InvertedIndexStorage invertedIndex(IndexerConfig config) {
        return switch (config.index()) {
            case "json" -> new MonolithicJsonIndexAdapter(config.datamarts().resolve("inverted_index.json"));
            case "folders" -> new FolderPerTermIndexAdapter(config.datamarts().resolve("inverted_index"));
            case "mongo" -> new MongodbIndexAdapter(config.mongoUri());
            default -> throw new IllegalArgumentException("Unknown index structure: " + config.index());
        };
    }

    public static MetadataStorage metadata(IndexerConfig config) {
        return switch (config.metadata()) {
            case "sqlite" -> new SqliteMetadataAdapter(config.datamarts().resolve("metadata.db"));
            case "mongo" -> new MongodbMetadataAdapter(config.mongoUri());
            default -> throw new IllegalArgumentException("Unknown metadata backend: " + config.metadata());
        };
    }
}
