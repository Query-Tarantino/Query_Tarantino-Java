package org.ulpgc.tarantino.crawler.adapters;

import org.ulpgc.tarantino.crawler.model.DownloadException;
import org.ulpgc.tarantino.crawler.model.FailureReason;
import org.ulpgc.tarantino.crawler.ports.BookDownloader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GutenbergHttpDownloader implements BookDownloader {

    private static final String URL_TEMPLATE = "https://www.gutenberg.org/cache/epub/%d/pg%d.txt";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    @Override
    public String download(int bookId) throws DownloadException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL_TEMPLATE.formatted(bookId, bookId)))
                .timeout(TIMEOUT)
                .header("User-Agent", "query-tarantino/1.0 (ULPGC Big Data course project)")
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return switch (response.statusCode()) {
                case 200 -> response.body();
                case 404 -> throw new DownloadException(FailureReason.NOT_FOUND, "Book " + bookId + " not found");
                default -> throw new DownloadException(FailureReason.NETWORK_ERROR,
                        "Unexpected HTTP " + response.statusCode() + " for book " + bookId);
            };
        } catch (IOException e) {
            throw new DownloadException(FailureReason.NETWORK_ERROR, "Could not download book " + bookId + ": " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DownloadException(FailureReason.NETWORK_ERROR, "Interrupted while downloading book " + bookId);
        }
    }
}
