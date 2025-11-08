package ru.spbstu.ssa.kawaiikeeper.config;

import com.pengrad.telegrambot.TelegramBot;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
@Configuration
public class BotConfig {

    private final ApiConfig apiConfig;

    @Bean
    public RestClient restClient() {
        return RestClient.builder()
            .baseUrl(apiConfig.getApiBaseUrl())
            .defaultHeader(HttpHeaders.USER_AGENT, "Kawaii-Keeper/0.1")
            .build();
    }

    @Bean
    public TelegramBot telegramBot() {
        return new TelegramBot(apiConfig.getBotToken());
    }

}
