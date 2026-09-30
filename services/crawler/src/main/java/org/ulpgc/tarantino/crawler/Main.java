package org.ulpgc.tarantino.crawler;

import org.ulpgc.tarantino.crawler.commands.IngestBookCommand;

public class Main {

    public static void main(String[] args) {
        IngestBookCommand ingest = CrawlerFactory.ingestCommand(CrawlerConfig.fromEnvironment());
        for (String bookId : args) {
            System.out.println(ingest.execute(Integer.parseInt(bookId)));
        }
    }
}
