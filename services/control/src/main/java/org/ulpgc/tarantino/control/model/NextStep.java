package org.ulpgc.tarantino.control.model;

public record NextStep(Action action, int bookId) {

    public enum Action {
        INDEX,
        DOWNLOAD,
        IDLE
    }
}
