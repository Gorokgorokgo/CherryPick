package com.cherrypick.app;

import me.paulschwarz.springdotenv.environment.DotenvPropertySource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.ConfigurableEnvironment;

@SpringBootApplication
public class CherrypickApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CherrypickApplication.class);
        app.addInitializers(applicationContext -> {
            ConfigurableEnvironment environment = applicationContext.getEnvironment();
            environment.getPropertySources().addFirst(new DotenvPropertySource());
        });
        app.run(args);
    }
}