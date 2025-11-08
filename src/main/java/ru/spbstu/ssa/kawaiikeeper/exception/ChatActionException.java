package ru.spbstu.ssa.kawaiikeeper.exception;

import lombok.Getter;

public class ChatActionException extends RuntimeException {

    @Getter
    private final long chatId;

    public ChatActionException(long chatId, String message) {
        super(message);
        this.chatId = chatId;
    }

    public ChatActionException(long chatId, String message, Throwable cause) {
        super(message, cause);
        this.chatId = chatId;
    }
}
