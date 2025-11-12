package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.EditMessageMedia;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.common.Callbacks;
import ru.spbstu.ssa.kawaiikeeper.dto.SavedDto;
import ru.spbstu.ssa.kawaiikeeper.exception.ChatActionException;
import ru.spbstu.ssa.kawaiikeeper.service.SavedService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Service
public class SavedHandler implements ChatEventHandler {

    private static final String SAVED_COMMAND = "saved";
    private static final String SET_PAGE_CALLBACK = "saved_page";
    private static final String REMOVE_SAVED_CALLBACK = "saved_remove";

    private final SavedService savedService;

    @Override
    public @NotNull Map< String, Function< ? super Message, List< ? extends BaseRequest< ?, ? > > > > commandHandlers() {
        return Map.of(
            SAVED_COMMAND, this::handleSaved
        );
    }

    @Override
    public @NotNull Map< String, Function< ? super CallbackQuery, List< ? extends BaseRequest< ?, ? > > > > callbackHandlers() {
        return Map.of(
            SET_PAGE_CALLBACK, this::handleSetPage,
            REMOVE_SAVED_CALLBACK, this::handleRemove
        );
    }

    private List< ? extends BaseRequest< ?, ? > > handleSaved(@NonNull Message message) {
        long chatId = message.chat().id();
        long userId = message.from().id();

        if (!savedService.hasImages(userId)) {
            log.info("Not found saved for userId={}", userId);
            return List.of(getNotFoundMessage(chatId));
        }

        List< SavedDto > userSaved = savedService.findOrderedImages(userId);
        SavedPageInfo pageInfo = new SavedPageInfo(userSaved, 0);
        InlineKeyboardMarkup keyboard = formSavedImageKeyboard(pageInfo);

        log.info("Start saved for userId={}", userId);
        return List.of(
            new SendPhoto(chatId, pageInfo.saved.imageUrl())
                .replyMarkup(keyboard)
        );
    }

    private List< ? extends BaseRequest< ?, ? > > handleSetPage(@NonNull CallbackQuery query) {
        long chatId = query.maybeInaccessibleMessage().chat().id();
        long userId = query.from().id();
        int messageId = query.maybeInaccessibleMessage().messageId();

        SavedPageInfo pageInfo;
        try {
            List< SavedDto > userSaved = savedService.findOrderedImages(userId);
            pageInfo = new SavedPageInfo(userSaved, Integer.parseInt(Callbacks.dataOf(query.data()).orElseThrow()));
        } catch (Exception e) {
            throw new ChatActionException(chatId, "Не удалось загрузить страницу", e);
        }

        InlineKeyboardMarkup keyboard = formSavedImageKeyboard(pageInfo);

        log.info("Set page {} for userId={}", pageInfo.currPage, userId);
        return List.of(
            new EditMessageMedia(chatId, messageId, new InputMediaPhoto(pageInfo.saved.imageUrl()))
                .replyMarkup(keyboard)
        );
    }

    private List< ? extends BaseRequest< ?, ? > > handleRemove(@NonNull CallbackQuery query) {
        long chatId = query.maybeInaccessibleMessage().chat().id();
        long userId = query.from().id();

        long removeId = Long.parseLong(Callbacks.dataOf(query.data()).orElseThrow());
        savedService.removeImage(removeId);

        log.info("Remove saved (id={}) for userId={}", removeId, userId);
        return List.of(
            new SendMessage(chatId, "Успешно убрано из коллекции")
        );
    }

    private InlineKeyboardMarkup formSavedImageKeyboard(SavedPageInfo pageInfo) {
        var keyboard = new InlineKeyboardMarkup();

        List< InlineKeyboardButton > controlRow = new ArrayList<>();
        if (pageInfo.prevPage != null) {
            controlRow.add(new InlineKeyboardButton(UnicodeEmoji.LEFT_ARROW.toString())
                .callbackData(Callbacks.callback(SET_PAGE_CALLBACK, pageInfo.prevPage.toString())));
        }
        if (pageInfo.nextPage != null) {
            controlRow.add(new InlineKeyboardButton(UnicodeEmoji.RIGHT_ARROW.toString())
                .callbackData(Callbacks.callback(SET_PAGE_CALLBACK, pageInfo.nextPage.toString())));
        }
        keyboard.addRow(controlRow.toArray(new InlineKeyboardButton[0]));

        keyboard.addRow(new InlineKeyboardButton(UnicodeEmoji.BROKEN_HEART + " Больше не нравится")
            .callbackData(Callbacks.callback(REMOVE_SAVED_CALLBACK, pageInfo.saved.id().toString())));

        return keyboard;
    }

    private SendMessage getNotFoundMessage(long chatId) {
        return new SendMessage(chatId, "Ваша коллекция пуста.");
    }

    @Getter
    @ToString
    private static class SavedPageInfo {

        private final SavedDto saved;
        private final Integer prevPage;
        private final Integer currPage;
        private final Integer nextPage;

        public SavedPageInfo(List< SavedDto > allSaved, int page) {
            saved = allSaved.get(page);
            prevPage = (page != 0) ? page - 1 : null;
            currPage = page;
            nextPage = (page != allSaved.size() - 1) ? page + 1 : null;
        }

    }

}
