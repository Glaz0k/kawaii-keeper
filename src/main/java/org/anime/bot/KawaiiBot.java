package org.anime.bot;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ReplyKeyboardMarkup;
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
        Message message = update.message();
        if (message != null) {
            String text = message.text();
            long chatId = message.chat().id();
            long userId = message.from().id();
            if (text.startsWith("/")) {
                handleCommand(userId, chatId, text);
            } else {
                handleMessage(userId, chatId, text);
            }
            bot.execute(new SendPhoto(chatId, backend.getImage(userId)));
        }

    }
    private void handleCommand(long userId, long chatId, String text) {
        String command = text.substring(1);
        if (validCommands.contains(command)) {
            switch (command) {
                case "start" -> {
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
    private void handleMessage(long userId, long chatId, String text) {
        switch (text) {
            case saveButtonText ->
                backend.saveImage(userId);
            case rejectButtonText ->
                backend.removeImage(userId);
            default ->
                bot.execute(new SendMessage(chatId, defaultResponse));
        }
    }

    private final TelegramBot bot;
    private final BotBackend backend;

    private static final String saveButtonText = "Нравится!";
    private static final String rejectButtonText = "Ну нет...";
    private static final ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup(
            new String[][] {{saveButtonText, rejectButtonText}}
    ).resizeKeyboard(true);
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