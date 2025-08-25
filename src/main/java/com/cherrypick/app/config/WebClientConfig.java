package com.cherrypick.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder()
                .defaultHeader("User-Agent", "CherryPick-OAuth/1.0")
                .codecs(configurer -> 
                    configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB buffer
                );
    }
}