package org.ulpgc.tarantino.crawler;

import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        CrawlerConfig config = CrawlerConfig.fromEnvironment();
        CrawlerFactory.removeIncompleteWrites(config);
        IngestBookCommand ingest = CrawlerFactory.ingestCommand(config);
        Arrays.stream(args).map(Integer::parseInt).map(ingest::execute).forEach(System.out::println);
    }
}
