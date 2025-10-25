package ru.spbstu.ssa.kawaiikeeper.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.exception.UserActionException;
import ru.spbstu.ssa.kawaiikeeper.handler.ChatEventHandler;
import ru.spbstu.ssa.kawaiikeeper.handler.Callbacks;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
@RequiredArgsConstructor
@Service
public class BotService {

    private final TelegramBot bot;
    private final List< ChatEventHandler > availableHandlers;

    private final Map< String, Consumer< ? super Message > > commandHandlers = new HashMap<>();
    private final Map< String, Consumer< ? super CallbackQuery > > callbackHandlers = new HashMap<>();

    @PostConstruct
    private void registerBot() {
        log.info("Registering EventHandlers");
        availableHandlers.forEach(this::registerChatEventHandler);

        log.info("Available commands:");
        commandHandlers.keySet().forEach(log::info);

        log.info("Handled callbacks:");
        callbackHandlers.keySet().forEach(log::info);

        log.info("Registering bot listener");
        bot.setUpdatesListener(updates -> {
            updates.forEach(this::handleUpdate);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void registerChatEventHandler(@NonNull ChatEventHandler chatEventHandler) {
        log.info("Registering {}", chatEventHandler.getClass().getSimpleName());
        commandHandlers.putAll(chatEventHandler.commandHandlers());
        callbackHandlers.putAll(chatEventHandler.callbackHandlers());
    }

    private void handleUpdate(@NonNull Update update) {
        try {
            if (update.message() != null) {
                handleMessage(update.message());
            } else if (update.callbackQuery() != null) {
                handleCallbackQuery(update.callbackQuery());
            }
        } catch (UserActionException e) {
            log.error(e.getMessage(), e);
            bot.execute(new SendMessage(e.getChatId(), e.getMessage()));
        }
    }

    private void handleMessage(@NonNull Message message) {
        String command = message.text().substring(1);
        var handler = commandHandlers.get(command);
        if (handler != null) {
            handler.accept(message);
        }
    }

    private void handleCallbackQuery(@NonNull CallbackQuery query) {
        String identifier = Callbacks.identifierOf(query.data());
        var handler = callbackHandlers.get(identifier);
        if (handler != null) {
            handler.accept(query);
        }
    }

}
