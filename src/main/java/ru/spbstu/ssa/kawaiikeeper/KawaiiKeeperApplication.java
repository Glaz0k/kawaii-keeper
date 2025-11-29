package ru.spbstu.ssa.kawaiikeeper;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.spbstu.ssa.kawaiikeeper.config.ApiConfig;

@EnableConfigurationProperties(ApiConfig.class)
@SpringBootApplication
public class KawaiiKeeperApplication {

    public static void main(String[] args) {
        SpringApplication.run(KawaiiKeeperApplication.class, args);
    }
}
