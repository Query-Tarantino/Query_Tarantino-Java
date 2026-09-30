package org.ulpgc.tarantino.control.model;

public record NextStep(Action action, int bookId) {

    public enum Action {
        INDEX,
        DOWNLOAD,
        IDLE
    }

    public static NextStep index(int bookId) {
        return new NextStep(Action.INDEX, bookId);
    }

    public static NextStep download(int bookId) {
        return new NextStep(Action.DOWNLOAD, bookId);
    }

    public static NextStep idle() {
        return new NextStep(Action.IDLE, 0);
    }
}
