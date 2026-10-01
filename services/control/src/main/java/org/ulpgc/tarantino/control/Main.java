package org.ulpgc.tarantino.control;

import org.ulpgc.tarantino.control.adapters.FileControlStateStore;
import org.ulpgc.tarantino.control.adapters.LocalCrawler;
import org.ulpgc.tarantino.control.adapters.LocalIndexer;
import org.ulpgc.tarantino.control.commands.ControlPipeline;
import org.ulpgc.tarantino.control.model.StepReport;
import org.ulpgc.tarantino.crawler.CrawlerConfig;
import org.ulpgc.tarantino.crawler.CrawlerFactory;
import org.ulpgc.tarantino.indexer.IndexerConfig;
import org.ulpgc.tarantino.indexer.IndexerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public class Main {

    private static final String DEFAULT_CANDIDATES = "sample_ids.txt";

    public static void main(String[] args) {
        ControlConfig config = ControlConfig.fromEnvironment();
        ControlPipeline pipeline = pipeline(config, candidatesFile(config, args));
        Stream.generate(pipeline::runStep).takeWhile(report -> !report.idle()).forEach(Main::print);
        System.out.println("[CONTROL] Nothing left to do");
    }

    private static ControlPipeline pipeline(ControlConfig config, Path candidatesFile) {
        CrawlerConfig crawlerConfig = CrawlerConfig.fromEnvironment();
        CrawlerFactory.removeIncompleteWrites(crawlerConfig);
        return new ControlPipeline(new FileControlStateStore(config.control()),
                new LocalCrawler(CrawlerFactory.ingestCommand(crawlerConfig)),
                new LocalIndexer(IndexerFactory.indexCommand(IndexerConfig.fromEnvironment())),
                candidates(candidatesFile));
    }

    private static Path candidatesFile(ControlConfig config, String[] args) {
        return config.workload().resolve(args.length > 0 ? args[0] : DEFAULT_CANDIDATES);
    }

    private static List<Integer> candidates(Path file) {
        return lines(file).stream()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .map(Integer::valueOf)
                .toList();
    }

    private static List<String> lines(Path file) {
        try {
            return Files.readAllLines(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void print(StepReport report) {
        System.out.println("[CONTROL] " + report.description());
    }
}
