package ru.spbstu.ssa.kawaiikeeper.exception;

import lombok.Getter;

public class UserActionException extends RuntimeException {

    @Getter
    private final long chatId;

    public UserActionException(long chatId, String message) {
        super(message);
        this.chatId = chatId;
    }

    public UserActionException(long chatId, String message, Throwable cause) {
        super(message, cause);
        this.chatId = chatId;
    }
}
