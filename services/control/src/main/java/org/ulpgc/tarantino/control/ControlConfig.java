package org.ulpgc.tarantino.control;

import java.nio.file.Path;

public record ControlConfig(Path control, Path workload, int indexBatch) {

    private static final String INDEX_BATCH = "TARANTINO_INDEX_BATCH";

    public static ControlConfig fromEnvironment() {
        return new ControlConfig(
                Path.of(variable("TARANTINO_CONTROL", "control")),
                Path.of(variable("TARANTINO_WORKLOAD", "workload")),
                indexBatch(variable(INDEX_BATCH, "100")));
    }

    static int indexBatch(String value) {
        try {
            int batch = Integer.parseInt(value.strip());
            if (batch > 0) {
                return batch;
            }
        } catch (NumberFormatException e) {
            // reported below with the offending value
        }
        throw new IllegalArgumentException("Unknown " + INDEX_BATCH + ": " + value + " (expected a positive integer)");
    }

    private static String variable(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue);
    }
}
