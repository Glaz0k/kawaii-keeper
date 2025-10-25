package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import org.springframework.lang.NonNull;

import java.util.Map;
import java.util.function.Consumer;

public interface ChatEventHandler {

    default @NonNull Map< String, Consumer< ? super Message > > commandHandlers() {
        return Map.of();
    }

    default @NonNull Map< String, Consumer< ? super CallbackQuery > > callbackHandlers() {
        return Map.of();
    }

}
