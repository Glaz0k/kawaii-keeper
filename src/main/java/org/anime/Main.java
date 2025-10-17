package org.anime;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.request.SendPhoto;

public class Main {
    static void main() {
        var bot = new TelegramBot("hahalol");
        IO.println("Hello and welcome!");
        for (int i = 1; i <= 5; i++) {
            IO.println("i = " + i);
        }
    }
}
