package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.common.Callbacks;
import ru.spbstu.ssa.kawaiikeeper.service.SavedService;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Service
public class ClearHandler implements ChatEventHandler {

    private static final String CLEAR_COMMAND = "clear";
    private static final String CONFIRM_CLEAR_CALLBACK = "clear_confirm";

    private final SavedService savedService;

    @Override
    public @NotNull Map< String, Function< ? super Message, Optional< ? extends BaseRequest< ?, ? > > > > commandHandlers() {
        return Map.of(
            CLEAR_COMMAND, this::handleClear
        );
    }

    @Override
    public @NotNull Map< String, Function< ? super CallbackQuery, Optional< ? extends BaseRequest< ?, ? > > > > callbackHandlers() {
        return Map.of(
            CONFIRM_CLEAR_CALLBACK, this::handleClearConfirm
        );
    }

    private Optional< SendMessage > handleClear(@NonNull Message message) {
        long chatId = message.chat().id();
        long userId = message.from().id();

        if (!savedService.hasImages(userId)) {
            return Optional.of(
                new SendMessage(chatId, "Ваша коллекция и так пуста...")
            );
        }

        log.info("Clear request for userId={}", userId);
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
            new InlineKeyboardButton(UnicodeEmoji.DISAPPOINTED_FACE + " Очищаем")
                .callbackData(Callbacks.callback(CONFIRM_CLEAR_CALLBACK))
        );
        return Optional.of(
            new SendMessage(chatId, "Вы уверены, что хотите очистить коллекцию?")
                .replyMarkup(keyboard)
        );
    }

    private Optional< SendMessage > handleClearConfirm(@NonNull CallbackQuery query) {
        long chatId = query.maybeInaccessibleMessage().chat().id();
        long userId = query.from().id();

        savedService.clearImages(userId);

        log.info("Clear collection for userId={}", userId);
        return Optional.of(new SendMessage(chatId, "Ваша коллекция была очищена. Надеемся вы найдете что-то более стоящее!"));
    }
}
