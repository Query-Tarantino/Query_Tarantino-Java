package org.ulpgc.tarantino.crawler.adapters;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ulpgc.tarantino.crawler.model.DownloadException;
import org.ulpgc.tarantino.crawler.model.FailureReason;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GutenbergHttpDownloaderTest {

    private HttpServer server;
    private GutenbergHttpDownloader downloader;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/cache/epub/", exchange -> {
            int status = exchange.getRequestURI().getPath().contains("/404/") ? 404
                    : exchange.getRequestURI().getPath().contains("/500/") ? 500 : 200;
            byte[] body = "Café *** START".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, body.length);
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        downloader = new GutenbergHttpDownloader("http://localhost:" + server.getAddress().getPort() + "/cache/epub/%d/pg%d.txt");
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void returnsTheBodyDecodedAsUtf8() {
        assertEquals("Café *** START", downloader.rawText(1342));
    }

    @Test
    void mapsMissingBooksToNotFound() {
        assertEquals(FailureReason.NOT_FOUND, assertThrows(DownloadException.class, () -> downloader.rawText(404)).reason());
    }

    @Test
    void mapsServerErrorsToNetworkError() {
        assertEquals(FailureReason.NETWORK_ERROR, assertThrows(DownloadException.class, () -> downloader.rawText(500)).reason());
    }

    @Test
    void mapsUnreachableServersToNetworkError() {
        GutenbergHttpDownloader unreachable = new GutenbergHttpDownloader("http://localhost:1/%d/%d.txt");

        assertEquals(FailureReason.NETWORK_ERROR, assertThrows(DownloadException.class, () -> unreachable.rawText(1)).reason());
    }
}
