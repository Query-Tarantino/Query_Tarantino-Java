package org.ulpgc.tarantino.crawler.ports;

import org.ulpgc.tarantino.crawler.model.DownloadException;

public interface BookDownloader {

    String download(int bookId) throws DownloadException;
}
