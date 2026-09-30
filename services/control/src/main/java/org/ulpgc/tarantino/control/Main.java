package org.ulpgc.tarantino.control;

import org.ulpgc.tarantino.control.adapters.FileControlStateStore;
import org.ulpgc.tarantino.control.commands.ControlPipeline;
import org.ulpgc.tarantino.control.model.NextStep;
import org.ulpgc.tarantino.crawler.CrawlerConfig;
import org.ulpgc.tarantino.crawler.CrawlerFactory;
import org.ulpgc.tarantino.indexer.IndexerConfig;
import org.ulpgc.tarantino.indexer.IndexerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class Main {

    public static void main(String[] args) throws IOException {
        ControlConfig config = ControlConfig.fromEnvironment();
        String idsFile = args.length > 0 ? args[0] : "sample_ids.txt";
        List<Integer> candidates = Files.readAllLines(config.workload().resolve(idsFile)).stream()
                .filter(line -> !line.isBlank())
                .map(line -> Integer.parseInt(line.strip()))
                .toList();

        ControlPipeline pipeline = new ControlPipeline(
                new FileControlStateStore(config.control()),
                CrawlerFactory.ingestCommand(CrawlerConfig.fromEnvironment()),
                IndexerFactory.indexCommand(IndexerConfig.fromEnvironment()),
                candidates);

        NextStep step;
        do {
            step = pipeline.runStep();
            System.out.println("[CONTROL] " + step);
        } while (step.action() != NextStep.Action.IDLE);
    }
}
