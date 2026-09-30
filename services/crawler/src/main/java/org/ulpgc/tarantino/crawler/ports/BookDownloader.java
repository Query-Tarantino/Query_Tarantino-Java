package org.ulpgc.tarantino.crawler.ports;

import org.ulpgc.tarantino.crawler.model.DownloadException;

public interface BookDownloader {

    String rawText(int bookId) throws DownloadException;
}
