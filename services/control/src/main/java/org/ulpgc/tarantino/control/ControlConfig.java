package org.ulpgc.tarantino.control;

import java.nio.file.Path;

public record ControlConfig(Path control, Path workload) {

    public static ControlConfig fromEnvironment() {
        return new ControlConfig(
                Path.of(env("TARANTINO_CONTROL", "control")),
                Path.of(env("TARANTINO_WORKLOAD", "workload")));
    }

    private static String env(String name, String defaultValue) {
        return System.getenv().getOrDefault(name, defaultValue);
    }
}
