package org.ulpgc.tarantino.indexer.benchmarking.support;

import org.ulpgc.tarantino.crawler.benchmarking.support.BenchmarkPaths;
import org.ulpgc.tarantino.crawler.benchmarking.support.Directories;
import org.ulpgc.tarantino.indexer.IndexerConfig;
import org.ulpgc.tarantino.indexer.IndexerFactory;
import org.ulpgc.tarantino.indexer.ports.InvertedIndexStorage;
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

import java.nio.file.Path;

public final class BenchmarkStore {

    private static final String MONGO = "mongo";

    private final IndexerConfig config;
    private final boolean mongo;

    private BenchmarkStore(IndexerConfig config, boolean mongo) {
        this.config = config;
        this.mongo = mongo;
    }

    public static BenchmarkStore forIndex(String index, String name) {
        return new BenchmarkStore(config(name, index, "sqlite"), MONGO.equals(index));
    }

    public static BenchmarkStore forMetadata(String metadata, String name) {
        return new BenchmarkStore(config(name, "json", metadata), MONGO.equals(metadata));
    }

    public InvertedIndexStorage invertedIndex() {
        return IndexerFactory.invertedIndex(config);
    }

    public MetadataStorage metadata() {
        return IndexerFactory.metadata(config);
    }

    public Path datamarts() {
        return config.datamarts();
    }

    public String mongoUri() {
        return config.mongoUri();
    }

    public void clear() {
        Directories.delete(config.datamarts());
        if (mongo) {
            MongoStores.drop(config.mongoUri());
        }
    }

    public long diskUsage() {
        return mongo ? MongoStores.diskUsage(config.mongoUri()) : Directories.diskUsage(config.datamarts());
    }

    private static IndexerConfig config(String name, String index, String metadata) {
        return new IndexerConfig(Path.of("datalake"), "time", BenchmarkPaths.scratch(name), index, metadata,
                BenchmarkPaths.mongoUri(name.replace('-', '_')), BenchmarkPaths.workload());
    }
}
