package ru.spbstu.ssa.kawaiikeeper.handler;

import org.springframework.lang.NonNull;

public enum UnicodeEmoji {
    LEFT_ARROW("\u2B05\uFE0F"),
    RIGHT_ARROW("\u27A1\uFE0F"),
    CANCEL("\u274C"),
    HEART("\u2764\uFE0F"),
    BROKEN_HEART("\uD83D\uDC94"),
    DISAPPOINTED_FACE("\uD83D\uDE1E");

    public final String value;

    UnicodeEmoji(@NonNull String unicodeEmoji) {
        this.value = unicodeEmoji;
    }

    @Override
    public String toString() {
        return value;
    }
}
