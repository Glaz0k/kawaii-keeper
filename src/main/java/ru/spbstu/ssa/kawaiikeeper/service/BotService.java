package ru.spbstu.ssa.kawaiikeeper.service;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.BotCommand;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetMyCommands;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.common.Callbacks;
import ru.spbstu.ssa.kawaiikeeper.exception.ChatActionException;
import ru.spbstu.ssa.kawaiikeeper.handler.ChatEventHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Service
public class BotService {

    private final TelegramBot bot;
    private final List< ChatEventHandler > availableHandlers;

    private final Map< String, Function< ? super Message, List< ? extends BaseRequest< ?, ? > > > > commandHandlers = new HashMap<>();
    private final Map< String, Function< ? super CallbackQuery, List< ? extends BaseRequest< ?, ? > > > > callbackHandlers = new HashMap<>();

    @PostConstruct
    private void registerBot() {
        log.info("Registering EventHandlers");
        availableHandlers.forEach(this::registerChatEventHandler);

        log.info("Available commands:");
        commandHandlers.keySet().forEach(log::info);

        log.info("Handled callbacks:");
        callbackHandlers.keySet().forEach(log::info);

        log.info("Setting up commands");
        setUpCommands();

        log.info("Registering bot listener");
        bot.setUpdatesListener(updates -> {
                log.info("Updates received: {}", updates.size());
                updates.forEach(this::handleUpdate);
                return UpdatesListener.CONFIRMED_UPDATES_ALL;
            },
            e -> log.error("Telegram exception", e)
        );
        log.info("Bot successfully registered");
    }

    private void registerChatEventHandler(@NonNull ChatEventHandler chatEventHandler) {
        log.info("Registering {}", chatEventHandler.getClass().getSimpleName());
        commandHandlers.putAll(chatEventHandler.commandHandlers());
        callbackHandlers.putAll(chatEventHandler.callbackHandlers());
    }

    private void setUpCommands() {
        BotCommand[] commands = {
            new BotCommand("/start", "Начать ленту"),
            new BotCommand("/category", "Выбор категории"),
            new BotCommand("/saved", "Моя коллекция"),
            new BotCommand("/clear", "Очистить коллекцию")
        };

        SetMyCommands setCommands = new SetMyCommands(commands);
        bot.execute(setCommands);
    }

    private void handleUpdate(@NonNull Update update) {
        try {
            if (update.message() != null) {
                handleMessage(update.message());
            } else if (update.callbackQuery() != null) {
                handleCallbackQuery(update.callbackQuery());
            }
        } catch (ChatActionException e) {
            log.warn("Exception occurred: {}", e.getMessage());
            bot.execute(new SendMessage(e.getChatId(), e.getMessage()));
        }
    }

    private void handleMessage(@NonNull Message message) {
        String command = message.text().substring(1);
        log.info("Received command {} from userId={}", command, message.from().id());
        var handler = commandHandlers.get(command);
        handlePrepared(handler, message);
    }

    private void handleCallbackQuery(@NonNull CallbackQuery query) {
        String identifier = Callbacks.identifierOf(query.data());
        log.info("Received callback {} from userId={}", identifier, query.from().id());
        var handler = callbackHandlers.get(identifier);
        handlePrepared(handler, query);
        bot.execute(new AnswerCallbackQuery(query.id()));
    }

    private < T > void handlePrepared(@Nullable Function< ? super T, List< ? extends BaseRequest< ?, ? > > > handler,
                                      T preparedData) {
        if (handler == null) {
            return;
        }
        handler.apply(preparedData)
            .forEach(req -> {
                log.info("Sending {} request", req.getClass().getSimpleName());
                var response = bot.execute(req);
                if (!response.isOk()) {
                    log.error("Request failed with code {}. {}", response.errorCode(), response.description());
                }
            });
    }

}
