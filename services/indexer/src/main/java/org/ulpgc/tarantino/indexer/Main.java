package org.ulpgc.tarantino.indexer;

import org.ulpgc.tarantino.indexer.commands.IndexBookCommand;

public class Main {

    public static void main(String[] args) {
        IndexBookCommand index = IndexerFactory.indexCommand(IndexerConfig.fromEnvironment());
        for (String bookId : args) {
            System.out.println(index.execute(Integer.parseInt(bookId)));
        }
    }
}
