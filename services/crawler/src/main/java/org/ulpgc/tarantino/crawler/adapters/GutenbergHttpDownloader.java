package org.ulpgc.tarantino.crawler.adapters;

import org.ulpgc.tarantino.crawler.model.DownloadException;
import org.ulpgc.tarantino.crawler.ports.BookDownloader;

public class GutenbergHttpDownloader implements BookDownloader {

    private static final String URL_TEMPLATE = "https://www.gutenberg.org/cache/epub/%d/pg%d.txt";

    @Override
    public String download(int bookId) throws DownloadException {
        // TODO: GET URL_TEMPLATE with java.net.http.HttpClient, map 404 -> NOT_FOUND, IOException -> NETWORK_ERROR
        throw new UnsupportedOperationException("TODO");
    }
}
