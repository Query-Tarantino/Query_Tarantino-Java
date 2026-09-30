package org.ulpgc.tarantino.control;

import java.nio.file.Path;

public record ControlConfig(Path control, Path workload) {

    public static ControlConfig fromEnvironment() {
        return new ControlConfig(
                Path.of(variable("TARANTINO_CONTROL", "control")),
                Path.of(variable("TARANTINO_WORKLOAD", "workload")));
    }

    private static String variable(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue);
    }
}
