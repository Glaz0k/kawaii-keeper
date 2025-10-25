package ru.spbstu.ssa.kawaiikeeper.config;

import com.pengrad.telegrambot.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.Objects;

@RequiredArgsConstructor
@Configuration
public class BotConfig {

    private final ApiConfig apiConfig;

    @Bean
    public RestClient restClient() {
        return RestClient.create(apiConfig.getApiBaseUrl());
    }

    @Bean
    public TelegramBot telegramBot(@Value("${api-config.bot-token}") String botToken) {
        Objects.requireNonNull(botToken, "Bot token is null");
        return new TelegramBot(botToken);
    }

}
