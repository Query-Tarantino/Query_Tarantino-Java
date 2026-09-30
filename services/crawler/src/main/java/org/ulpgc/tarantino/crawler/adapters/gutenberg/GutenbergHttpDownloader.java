package org.ulpgc.tarantino.crawler.adapters.gutenberg;

import org.ulpgc.tarantino.crawler.model.failure.DownloadException;
import org.ulpgc.tarantino.crawler.model.failure.FailureReason;
import org.ulpgc.tarantino.crawler.ports.BookDownloader;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class GutenbergHttpDownloader implements BookDownloader {

    private static final String MIRROR_URL_TEMPLATE = "https://mirror.cs.odu.edu/gutenberg-epub/%d/pg%d.txt";
    private static final String USER_AGENT = "query-tarantino/1.0 (ULPGC Big Data course project)";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String urlTemplate;
    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(TIMEOUT)
            .build();

    public GutenbergHttpDownloader() {
        this(MIRROR_URL_TEMPLATE);
    }

    public GutenbergHttpDownloader(String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    @Override
    public String rawText(int bookId) throws DownloadException {
        return body(response(request(bookId), bookId), bookId);
    }

    private HttpRequest request(int bookId) {
        return HttpRequest.newBuilder(URI.create(urlTemplate.formatted(bookId, bookId)))
                .timeout(TIMEOUT)
                .header("User-Agent", USER_AGENT)
                .build();
    }

    private HttpResponse<String> response(HttpRequest request, int bookId) {
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException | InterruptedException e) {
            restoreInterruption(e);
            throw new DownloadException(FailureReason.NETWORK_ERROR, "Could not download book " + bookId + ": " + e);
        }
    }

    private static void restoreInterruption(Exception e) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    private static String body(HttpResponse<String> response, int bookId) {
        return switch (response.statusCode()) {
            case 200 -> response.body();
            case 404 -> throw new DownloadException(FailureReason.NOT_FOUND, "Book " + bookId + " not found");
            default -> throw new DownloadException(FailureReason.NETWORK_ERROR, "HTTP " + response.statusCode() + " for book " + bookId);
        };
    }
}
