package org.ulpgc.tarantino.indexer;

import org.ulpgc.tarantino.indexer.commands.IndexBookCommand;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        IndexBookCommand index = IndexerFactory.indexCommand(IndexerConfig.fromEnvironment());
        index.execute(Arrays.stream(args).map(Integer::valueOf).toList()).forEach(System.out::println);
    }
}
