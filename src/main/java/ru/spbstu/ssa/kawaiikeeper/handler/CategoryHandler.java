package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.common.Callbacks;
import ru.spbstu.ssa.kawaiikeeper.config.ApiConfig;
import ru.spbstu.ssa.kawaiikeeper.exception.ChatActionException;
import ru.spbstu.ssa.kawaiikeeper.service.CategoryService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class CategoryHandler implements ChatEventHandler {

    private static final int PAGE_SIZE = 6;

    private static final String CATEGORY_COMMAND = "category";
    private static final String SET_PAGE_CALLBACK = "category_page";
    private static final String UPDATE_CATEGORY_CALLBACK = "category_update";
    private static final String CANCEL_CALLBACK = "category_cancel";

    private final CategoryService categoryService;
    private final List< String > availableCategories;
    private final int pageCount;

    public CategoryHandler(CategoryService categoryService, ApiConfig apiConfig) {
        this.categoryService = categoryService;
        this.availableCategories = apiConfig.getApiCategories();
        this.pageCount = (availableCategories.size() + PAGE_SIZE - 1) / PAGE_SIZE;
    }

    @Override
    public @NonNull Map< String, Function< ? super Message, List< ? extends BaseRequest< ?, ? > > > > commandHandlers() {
        return Map.of(
            CATEGORY_COMMAND, this::handleCategory
        );
    }

    @Override
    public @NonNull Map< String, Function< ? super CallbackQuery, List< ? extends BaseRequest< ?, ? > > > > callbackHandlers() {
        return Map.of(
            SET_PAGE_CALLBACK, this::handleSetPage,
            UPDATE_CATEGORY_CALLBACK, this::handleUpdateCategory,
            CANCEL_CALLBACK, this::handleCancel
        );
    }

    public List< SendMessage > handleCategory(@NonNull Message message) {
        long chatId = message.chat().id();

        String text = formCategoryPageText(0);
        InlineKeyboardMarkup keyboard = formCategoryPageKeyboard(0);

        return List.of(new SendMessage(chatId, text).replyMarkup(keyboard));
    }

    public List< EditMessageText > handleSetPage(@NonNull CallbackQuery query) {
        long chatId = query.maybeInaccessibleMessage().chat().id();
        int messageId = query.maybeInaccessibleMessage().messageId();
        int page = Integer.parseInt((Callbacks.dataOf(query.data()).orElseThrow()));

        String text = formCategoryPageText(page);
        InlineKeyboardMarkup keyboard = formCategoryPageKeyboard(page);

        return List.of(new EditMessageText(chatId, messageId, text).replyMarkup(keyboard));
    }

    public List< AnswerCallbackQuery > handleUpdateCategory(@NonNull CallbackQuery query) {
        long chatId = query.maybeInaccessibleMessage().chat().id();
        long userId = query.from().id();

        try {
            String updateCategory = Callbacks.dataOf(query.data()).orElseThrow();
            categoryService.updateCategory(userId, updateCategory);

            log.info("Update category to \"{}\" for userId={}", updateCategory, userId);
            return List.of(
                new AnswerCallbackQuery(query.id()).text("Категория успешно обновлена, теперь - %s".formatted(updateCategory))
            );
        } catch (Exception e) {
            throw new ChatActionException(chatId, "Не удалось обновить категорию", e);
        }
    }

    public List< DeleteMessage > handleCancel(@NonNull CallbackQuery query) {
        long chatId = query.maybeInaccessibleMessage().chat().id();
        long userId = query.from().id();
        int messageId = query.maybeInaccessibleMessage().messageId();

        log.info("Cancel category update for userId={}", userId);
        return List.of(new DeleteMessage(chatId, messageId));
    }

    private String formCategoryPageText(int page) {
        return "Страница - %d / %d".formatted(page + 1, pageCount);
    }

    private InlineKeyboardMarkup formCategoryPageKeyboard(int page) {
        if (page < 0 || page > pageCount - 1) {
            throw new IndexOutOfBoundsException("Page index out of range");
        }
        var keyboard = new InlineKeyboardMarkup();
        List< InlineKeyboardButton > controlRow = new ArrayList<>();
        if (page > 0) {
            controlRow.add(new InlineKeyboardButton(UnicodeEmoji.LEFT_ARROW.toString()).callbackData(Callbacks.callback(
                SET_PAGE_CALLBACK,
                String.valueOf(page - 1)
            )));
        }
        if (page < pageCount - 1) {
            controlRow.add(new InlineKeyboardButton(UnicodeEmoji.RIGHT_ARROW.toString()).callbackData(Callbacks.callback(
                SET_PAGE_CALLBACK,
                String.valueOf(page + 1)
            )));
        }
        keyboard.addRow(controlRow.toArray(new InlineKeyboardButton[0]));

        availableCategories.stream()
            .skip((long) page * PAGE_SIZE)
            .limit(PAGE_SIZE)
            .map(categoryName -> new InlineKeyboardButton(categoryName).callbackData(Callbacks.callback(UPDATE_CATEGORY_CALLBACK, categoryName)))
            .forEach(keyboard::addRow);

        keyboard.addRow(new InlineKeyboardButton(UnicodeEmoji.CANCEL + " Отмена").callbackData(Callbacks.callback(CANCEL_CALLBACK)));

        return keyboard;
    }

}
