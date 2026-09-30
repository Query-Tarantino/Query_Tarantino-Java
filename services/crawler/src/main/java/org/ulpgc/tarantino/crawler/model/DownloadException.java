package org.ulpgc.tarantino.crawler.model;

public class DownloadException extends RuntimeException {

    private final FailureReason reason;

    public DownloadException(FailureReason reason, String message) {
        super(message);
        this.reason = reason;
    }

    public FailureReason reason() {
        return reason;
    }
}
