package org.anime.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.CallbackQuery;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.message.MaybeInaccessibleMessage;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SendPhoto;

import java.util.Set;

public class KawaiiBot {
    public KawaiiBot(BotBackend backend, String botToken) {
        this.backend = backend;
        this.bot = new TelegramBot(botToken);
    }
    public void registerBot() {
        bot.setUpdatesListener(updates -> {
            for (Update update : updates) {
                handleUpdate(update);
            }
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }
    private void handleUpdate(Update update) {
        Long chatId = null;
        Long userId = null;
        Message message = update.message();
        if (message != null) {
            String text = message.text();
            chatId = message.chat().id();
            userId = message.from().id();
            if (text.startsWith("/")) {
                handleCommand(userId, chatId, text);
            } else {
                bot.execute(new SendMessage(chatId.longValue(), defaultResponse));
                chatId = null;
            }
        }
        CallbackQuery callback = update.callbackQuery();
        if (callback != null) {
            MaybeInaccessibleMessage callbackMessage = callback.maybeInaccessibleMessage();
            chatId = callbackMessage.chat().id();
            userId = callback.from().id();
            switch (callback.inlineMessageId()) {
                case saveButtonId -> backend.saveImage(userId);
                case rejectButtonId -> backend.removeImage(userId);
                default -> {}
            }
        }
        if (chatId != null) {
            bot.execute(new SendPhoto(chatId.longValue(), backend.getImage(userId)));
        }
    }
    private void handleCommand(long userId, long chatId, String text) {
        String command = text.substring(1);
        if (validCommands.contains(command)) {
            switch (command) {
                case "start" -> {
                    InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(
                            new InlineKeyboardButton(saveButtonText, saveButtonId),
                            new InlineKeyboardButton(rejectButtonText, rejectButtonId)
                    );
                    backend.setTag(userId, "random");
                    bot.execute(new SendMessage(chatId, greetResponse).replyMarkup(keyboard));
                } case "clear" -> {
                    backend.clearSaved(userId);
                    bot.execute(new SendMessage(chatId, clearResponse));
                }
                case "saved" -> {
                    backend.setTag(userId, null);
                    bot.execute(new SendMessage(chatId, savedResponse));
                }
                default -> {
                    backend.setTag(userId, command);
                    bot.execute(new SendMessage(chatId, tagResponse + command));
                }
            }
        } else {
            bot.execute(new SendMessage(chatId, wrongCommandResponse));
        }
    }

    private TelegramBot bot;
    private BotBackend backend;
    private static final String saveButtonText = "Нравится!";
    private static final String rejectButtonText = "Ну нет...";
    private static final String saveButtonId = "SAVE";
    private static final String rejectButtonId = "REJECT";
    private static final String greetResponse = "Добро пожаловать! Используйте комманды из меню!";
    private static final String clearResponse = "Очищаем вашу коллекцию!";
    private static final String savedResponse = "Теперь покажем вашу личную коллекцию! Если нравится, то оставляем, а если же нет...";
    private static final String tagResponse = "Теперь показываем картинки с тегом: ";
    private static final String defaultResponse = "Мяф!";
    private static final String wrongCommandResponse = "Извините, такая комманда не поддерживается :(";
    private static final Set<String> validCommands = Set.of(
            "start",
            "random",
            "saved",
            "clear",
            "catgirl",
            "foxgirl",
            "wolf-girl",
            "animal-ears",
            "tail",
            "tail-with-ribbon",
            "cute",
            "cuteness-is-justice",
            "blue-archive",
            "girl",
            "young-girl",
            "maid",
            "maid-uniform",
            "vtuber",
            "w-sitting",
            "lying-down",
            "hands-forming-a-heart",
            "wink",
            "valentine",
            "headphones",
            "thigh-high-socks",
            "knee-high-socks",
            "white-tights",
            "black-tights",
            "heterochromia",
            "uniform",
            "sailor-uniform",
            "hoodie",
            "ribbon",
            "white-hair",
            "blue-hair",
            "long-hair",
            "blonde",
            "blue-eyes",
            "purple-eyes"
    );
}