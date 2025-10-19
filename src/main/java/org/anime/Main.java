package org.anime;

import org.anime.bot.BotBackend;
import org.anime.bot.KawaiiBot;

public class Main {
    static void main() {
        var bot = new KawaiiBot(new BotBackend() {
            @Override
            public String getImage(long userId) {
                return "https://i.redd.it/sfbyk901c82d1.jpeg";
            }

            @Override
            public void saveImage(long userId) {
                IO.println("Saving image");
            }

            @Override
            public void removeImage(long userId) {
                IO.println("Removing image");
            }

            @Override
            public void setTag(long userId, String tag) {
                IO.println("Setting tag to "+tag);
            }

            @Override
            public void clearSaved(long userId) {
                IO.println("Clearing saved");
            }
        }, System.getenv("BOT_KEY"));
        bot.registerBot();
    }
}
