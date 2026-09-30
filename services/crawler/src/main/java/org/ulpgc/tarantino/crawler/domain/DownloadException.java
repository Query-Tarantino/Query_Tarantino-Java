package org.ulpgc.tarantino.crawler.domain;

public class DownloadException extends RuntimeException {
    public DownloadException(String message) {
        super(message);
    }
}
