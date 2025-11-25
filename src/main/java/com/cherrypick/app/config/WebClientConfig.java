package com.cherrypick.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

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

    /**
     * 기본 WebClient Bean (카카오 로컬 API 등 외부 API 호출용)
     */
    @Bean
    public WebClient webClient() {
        // HTTP 클라이언트 타임아웃 설정
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofSeconds(10)); // 응답 타임아웃 10초

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader("User-Agent", "CherryPick-Location/1.0")
                .codecs(configurer ->
                        configurer.defaultCodecs().maxInMemorySize(1024 * 1024) // 1MB buffer
                )
                .build();
    }
}