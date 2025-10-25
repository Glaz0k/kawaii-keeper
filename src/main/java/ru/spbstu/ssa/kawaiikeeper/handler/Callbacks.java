package ru.spbstu.ssa.kawaiikeeper.handler;

import org.springframework.lang.NonNull;

public final class Callbacks {

    private static final char CALLBACK_DELIMITER = '.';

    public static @NonNull String callback(@NonNull String identifier, String data) {
        if (data == null) {
            return callback(identifier);
        }
        return identifier + CALLBACK_DELIMITER + data;
    }

    public static @NonNull String callback(@NonNull String identifier) {
        return identifier;
    }

    public static String dataOf(@NonNull String callback) {
        int index = callback.indexOf(CALLBACK_DELIMITER);
        return (index == -1) ? null : callback.substring(index + 1);
    }

    public static @NonNull String identifierOf(@NonNull String callback) {
        int index = callback.indexOf(CALLBACK_DELIMITER);
        return (index == -1) ? callback : callback.substring(0, index);
    }

    private Callbacks() {
    }

}
