package ru.spbstu.ssa.kawaiikeeper.handler;

import org.springframework.lang.Nullable;

import java.util.Optional;

public final class Callbacks {

    private static final char CALLBACK_DELIMITER = '.';

    private Callbacks() {
    }

    public static String callback(String identifier, @Nullable String data) {
        if (data == null) {
            return callback(identifier);
        }
        return identifier + CALLBACK_DELIMITER + data;
    }

    public static String callback(String identifier) {
        return identifier;
    }

    public static Optional<String> dataOf(String callback) {
        int index = callback.indexOf(CALLBACK_DELIMITER);
        return (index == -1) ? Optional.empty() : Optional.of(callback.substring(index + 1));
    }

    public static String identifierOf(String callback) {
        int index = callback.indexOf(CALLBACK_DELIMITER);
        return (index == -1) ? callback : callback.substring(0, index);
    }

}
