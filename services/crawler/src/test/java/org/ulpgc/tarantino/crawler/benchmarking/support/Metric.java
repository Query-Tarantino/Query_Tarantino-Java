package org.ulpgc.tarantino.crawler.benchmarking.support;

public record Metric(String name, String unit, Conversion conversion) {

    @FunctionalInterface
    public interface Conversion {
        double value(double score, int books);
    }

    public static Metric asMeasured(String name, String unit) {
        return new Metric(name, unit, (score, books) -> score);
    }

    public static Metric booksPerSecond(String name) {
        return new Metric(name, "books/s", (milliseconds, books) -> books / (milliseconds / 1000));
    }
}
