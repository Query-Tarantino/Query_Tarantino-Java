package org.ulpgc.tarantino.control;

import org.ulpgc.tarantino.control.adapters.FileControlStateStore;
import org.ulpgc.tarantino.control.adapters.LocalCrawler;
import org.ulpgc.tarantino.control.adapters.LocalIndexer;
import org.ulpgc.tarantino.control.commands.ControlPipeline;
import org.ulpgc.tarantino.crawler.CrawlerConfig;
import org.ulpgc.tarantino.crawler.CrawlerFactory;
import org.ulpgc.tarantino.indexer.IndexerConfig;
import org.ulpgc.tarantino.indexer.IndexerFactory;

import java.util.List;

public final class ControlFactory {

    private ControlFactory() {
    }

    /** The pipeline of the control service, once what an interrupted run left in the datalake is removed (SPEC §6). */
    public static ControlPipeline pipeline(ControlConfig config, CrawlerConfig crawler, IndexerConfig indexer,
                                           List<Integer> candidates) {
        CrawlerFactory.removeIncompleteWrites(crawler);
        return new ControlPipeline(new FileControlStateStore(config.control()),
                new LocalCrawler(CrawlerFactory.ingestCommand(crawler)),
                new LocalIndexer(IndexerFactory.indexCommand(indexer)),
                candidates, config.indexBatch());
    }
}
