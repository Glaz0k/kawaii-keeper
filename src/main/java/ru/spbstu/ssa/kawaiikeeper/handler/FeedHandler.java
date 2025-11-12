package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.request.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.common.Callbacks;
import ru.spbstu.ssa.kawaiikeeper.dto.ImageDto;
import ru.spbstu.ssa.kawaiikeeper.entity.Category;
import ru.spbstu.ssa.kawaiikeeper.exception.ChatActionException;
import ru.spbstu.ssa.kawaiikeeper.service.CategoryService;
import ru.spbstu.ssa.kawaiikeeper.service.ImageService;
import ru.spbstu.ssa.kawaiikeeper.service.SavedService;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
@Service
public class FeedHandler implements ChatEventHandler {

    private static final String START_COMMAND = "start";
    private static final String NEXT_CALLBACK = "feed_next";
    private static final String SAVE_CALLBACK = "feed_save";

    private final CategoryService categoryService;
    private final ImageService imageService;
    private final SavedService savedService;

    @Override
    public @NotNull Map< String, Function< ? super Message, List< ? extends BaseRequest< ?, ? > > > > commandHandlers() {
        return Map.of(
            START_COMMAND, this::handleStart
        );
    }

    @Override
    public @NotNull Map< String, Function< ? super CallbackQuery, List< ? extends BaseRequest< ?, ? > > > > callbackHandlers() {
        return Map.of(
            NEXT_CALLBACK, this::handleNext,
            SAVE_CALLBACK, this::handleSave
        );
    }

    private List< ? extends BaseRequest< ?, ? > > handleStart(@NonNull Message message) {
        long chatId = message.chat().id();
        long userId = message.from().id();

        if (!categoryService.hasCategory(userId)) {
            log.info("New user: userId={}", userId);
            Category userCategory = categoryService.setDefaultCategory(userId);
            return List.of(
                getGreetingsMessage(chatId),
                getFeedMessage(chatId, userCategory)
            );
        }

        log.info("Start feed for userId={}", userId);
        Category userCategory = categoryService
            .findCategory(userId)
            .orElseThrow();
        return List.of(getFeedMessage(chatId, userCategory));
    }

    private List< ? extends BaseRequest< ?, ? > > handleNext(@NonNull CallbackQuery query) {
        long userId = query.from().id();
        long chatId = query.maybeInaccessibleMessage().chat().id();
        int messageId = query.maybeInaccessibleMessage().messageId();

        log.info("Next feed for userId={}", userId);
        Category userCategory = categoryService
            .findCategory(userId)
            .orElseThrow(() -> new ChatActionException(chatId, "Не указана категория"));
        ImageDto image = getFeedImage(chatId, userCategory);
        InlineKeyboardMarkup keyboard = formFeedImageKeyboard(image);
        String caption = formFeedImageCaption(userCategory, image);

        var editImageReq = new EditMessageMedia(chatId, messageId, new InputMediaPhoto(image.imageUrl()));
        var editCaptionReq = new EditMessageCaption(chatId, messageId)
            .caption(caption)
            .replyMarkup(keyboard);

        return List.of(
            editImageReq,
            editCaptionReq
        );
    }

    private List< ? extends BaseRequest< ?, ? > > handleSave(@NonNull CallbackQuery query) {
        long userId = query.from().id();
        long chatId = query.maybeInaccessibleMessage().chat().id();
        int messageId = query.maybeInaccessibleMessage().messageId();

        String saveId = Callbacks.dataOf(query.data()).orElseThrow();
        try {
            savedService.saveImage(userId, saveId);
        } catch (Exception e) {
            throw new ChatActionException(chatId, "Не удалось сохранить", e);
        }

        log.info("Saved {} for userId={}", saveId, userId);
        return List.of(
            new AnswerCallbackQuery(query.id())
                .text("Изображение успешно сохранено :)"),
            new EditMessageReplyMarkup(chatId, messageId).replyMarkup(formSavedKeyboard())
        );
    }

    private SendPhoto getFeedMessage(long chatId, Category category) {
        ImageDto image = getFeedImage(chatId, category);
        String caption = formFeedImageCaption(category, image);
        InlineKeyboardMarkup keyboard = formFeedImageKeyboard(image);
        return new SendPhoto(chatId, image.imageUrl())
            .caption(caption)
            .replyMarkup(keyboard);
    }

    private ImageDto getFeedImage(long chatId, Category category) {
        try {
            return imageService.pollNext(category);
        } catch (Exception e) {
            throw new ChatActionException(chatId, "Не удалось получить фото");
        }
    }

    private InlineKeyboardMarkup formSavedKeyboard() {
        var keyboard = new InlineKeyboardMarkup();
        keyboard.addRow(
            new InlineKeyboardButton(UnicodeEmoji.THUMBS_UP + " Сохранено").callbackData(Callbacks.callback()),
            new InlineKeyboardButton("Дальше").callbackData(Callbacks.callback(NEXT_CALLBACK))
        );
        return keyboard;
    }

    private InlineKeyboardMarkup formFeedImageKeyboard(ImageDto image) {
        var keyboard = new InlineKeyboardMarkup();
        keyboard.addRow(
            new InlineKeyboardButton(UnicodeEmoji.HEART + " Сохранить").callbackData(Callbacks.callback(SAVE_CALLBACK, image.externalId())),
            new InlineKeyboardButton("Дальше").callbackData(Callbacks.callback(NEXT_CALLBACK))
        );
        return keyboard;
    }

    private String formFeedImageCaption(Category category, ImageDto nextImage) {
        String currentCategory = "Текущая категория: %s".formatted(category.getCategoryName());
        String imageMainCategory = "Тег изображения: %s".formatted(nextImage.categoryName());
        return currentCategory + "\n" + imageMainCategory;
    }

    private SendMessage getGreetingsMessage(long chatId) {
        return new SendMessage(chatId, "Добро пожаловать! Используйте команды из меню!");
    }
}
