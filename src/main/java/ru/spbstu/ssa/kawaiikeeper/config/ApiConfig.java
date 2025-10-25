package ru.spbstu.ssa.kawaiikeeper.config;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties("api-config")
public class ApiConfig {

    @NotEmpty(message = "Bot token must not be empty")
    private String botToken;

    @NotEmpty(message = "API base URL must not be empty")
    private String apiBaseUrl;

    @NotEmpty(message = "At least one API category must be specified")
    private List< String > apiCategories;

}
