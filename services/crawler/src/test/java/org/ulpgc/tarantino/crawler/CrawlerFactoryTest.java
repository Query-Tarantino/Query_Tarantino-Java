package org.ulpgc.tarantino.crawler;

import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.crawler.adapters.BatchBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.adapters.BookBasedDatalakeAdapter;
import org.ulpgc.tarantino.crawler.adapters.TimeBasedDatalakeAdapter;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CrawlerFactoryTest {

    @Test
    void selectsTheConfiguredDatalakeLayout() {
        assertInstanceOf(TimeBasedDatalakeAdapter.class, CrawlerFactory.datalake(config("time")));
        assertInstanceOf(BookBasedDatalakeAdapter.class, CrawlerFactory.datalake(config("book")));
        assertInstanceOf(BatchBasedDatalakeAdapter.class, CrawlerFactory.datalake(config("batch")));
    }

    @Test
    void rejectsUnknownLayouts() {
        assertThrows(IllegalArgumentException.class, () -> CrawlerFactory.datalake(config("hash")));
    }

    private static CrawlerConfig config(String layout) {
        return new CrawlerConfig(Path.of("datalake"), layout);
    }
}
