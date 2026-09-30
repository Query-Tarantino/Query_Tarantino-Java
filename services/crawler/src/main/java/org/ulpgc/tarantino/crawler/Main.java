package org.ulpgc.tarantino.crawler;

import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        IngestBookCommand ingest = CrawlerFactory.ingestCommand(CrawlerConfig.fromEnvironment());
        Arrays.stream(args).map(Integer::parseInt).map(ingest::execute).forEach(System.out::println);
    }
}
