package ru.spbstu.ssa.kawaiikeeper.handler;

import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import ru.spbstu.ssa.kawaiikeeper.dto.ImageDto;
import ru.spbstu.ssa.kawaiikeeper.entity.Category;
import ru.spbstu.ssa.kawaiikeeper.exception.ChatActionException;
import ru.spbstu.ssa.kawaiikeeper.service.CategoryService;
import ru.spbstu.ssa.kawaiikeeper.service.ImageService;
import ru.spbstu.ssa.kawaiikeeper.service.SavedService;

import java.util.Map;
import java.util.Optional;
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
    public @NotNull Map< String, Function< ? super Message, Optional< ? extends BaseRequest< ?, ? > > > > commandHandlers() {
        return Map.of(
            START_COMMAND, this::handleStart
        );
    }

    @Override
    public @NotNull Map< String, Function< ? super CallbackQuery, Optional< ? extends BaseRequest< ?, ? > > > > callbackHandlers() {
        return Map.of(
            NEXT_CALLBACK, this::handleNext,
            SAVE_CALLBACK, this::handleSave
        );
    }

    private Optional< ? extends BaseRequest< ?, ? > > handleStart(@NonNull Message message) {
        long chatId = message.chat().id();
        long userId = message.from().id();

        Optional< Category > category = categoryService.findCategory(userId);
        if (category.isEmpty()) {
            log.info("New user: userId={}", userId);
            categoryService.setDefaultCategory(userId);
            return Optional.of(getGreetings(chatId));
        }

        log.info("Start feed for userId={}", userId);
        return Optional.of(getFeedImage(chatId, category.get()));
    }

    private Optional< ? extends BaseRequest< ?, ? > > handleNext(@NonNull CallbackQuery query) {
        long userId = query.from().id();
        long chatId = query.maybeInaccessibleMessage().chat().id();

        Category current = categoryService
            .findCategory(userId)
            .orElseThrow(() -> new ChatActionException(chatId, "Не указана категория"));

        log.info("Next feed for userId={}", userId);
        return Optional.of(getFeedImage(chatId, current));
    }

    private Optional< ? extends BaseRequest< ?, ? > > handleSave(@NonNull CallbackQuery query) {
        long userId = query.from().id();
        long chatId = query.maybeInaccessibleMessage().chat().id();

        String saveId = Callbacks.dataOf(query.data());
        try {
            savedService.saveImage(userId, saveId);
        } catch (Exception e) {
            throw new ChatActionException(chatId, "Не удалось сохранить", e);
        }

        log.info("Saved {} for userId={}", saveId, userId);
        return Optional.of(new SendMessage(chatId, "Изображение успешно сохранено :)"));
    }

    private SendPhoto getFeedImage(long chatId, Category category) {
        log.info("Getting feed image for userId={}", category.getUserId());

        ImageDto nextImage;
        try {
            nextImage = imageService.pollNext(category);
        } catch (Exception e) {
            throw new ChatActionException(chatId, "Произошла ошибка при получении изображения", e);
        }
        log.info("Got {} for userId={}", nextImage, category.getUserId());

        var caption = formFeedImageCaption(category, nextImage);
        var keyboard = formFeedImageKeyboard(nextImage);
        return new SendPhoto(chatId, nextImage.imageUrl())
            .replyMarkup(keyboard)
            .caption(caption);
    }

    private InlineKeyboardMarkup formFeedImageKeyboard(ImageDto image) {
        var keyboard = new InlineKeyboardMarkup();
        keyboard.addRow(
            new InlineKeyboardButton("Нравится").callbackData(Callbacks.callback(SAVE_CALLBACK, image.externalId())),
            new InlineKeyboardButton("Следующая").callbackData(Callbacks.callback(NEXT_CALLBACK))
        );
        return keyboard;
    }

    private String formFeedImageCaption(Category category, ImageDto nextImage) {
        String currentCategory = "Текущая категория: %s".formatted(category.getCategoryName());
        String imageMainCategory = "Тег изображения: %s".formatted(nextImage.categoryName());
        return currentCategory + "\n" + imageMainCategory;
    }

    private SendMessage getGreetings(long chatId) {
        InlineKeyboardButton startFeedButton = new InlineKeyboardButton("Начнём").callbackData(Callbacks.callback(NEXT_CALLBACK));
        return new SendMessage(chatId, "Добро пожаловать! Используйте команды из меню!")
            .replyMarkup(new InlineKeyboardMarkup(startFeedButton));
    }
}
