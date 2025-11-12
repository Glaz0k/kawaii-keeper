package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.request.BaseRequest;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface ChatEventHandler {

    default @NonNull Map< String, Function< ? super Message, List< ? extends BaseRequest< ?, ? > > > > commandHandlers() {
        return Map.of();
    }

    default @NonNull Map< String, Function< ? super CallbackQuery, List< ? extends BaseRequest< ?, ? > > > > callbackHandlers() {
        return Map.of();
    }

}
