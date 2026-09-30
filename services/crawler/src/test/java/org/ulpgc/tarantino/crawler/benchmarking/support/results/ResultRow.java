package org.ulpgc.tarantino.crawler.benchmarking.support.results;

import java.util.Locale;

public record ResultRow(String structure, String metric, int books, double value, String unit) {

    public static final String LANGUAGE = "java";
    public static final String CSV_HEADER = "language,structure,metric,n_books,value,unit";

    public static ResultRow parse(String csvLine) {
        String[] fields = csvLine.split(",", -1);
        return new ResultRow(fields[1], fields[2], Integer.parseInt(fields[3]), Double.parseDouble(fields[4]), fields[5]);
    }

    public String csvLine() {
        return String.join(",", LANGUAGE, structure, metric, String.valueOf(books), String.format(Locale.ROOT, "%.3f", value), unit);
    }
}
