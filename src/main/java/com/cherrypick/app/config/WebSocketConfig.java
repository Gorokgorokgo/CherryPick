package com.cherrypick.app.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * 순수 WebSocket 설정 클래스 (STOMP 없음)
 * JSON 메시지 기반 실시간 통신 지원
 * 
 * 주요 기능:
 * - 실시간 입찰 업데이트
 * - 경매 상태 변경 알림
 * - 낙찰 알림
 * - React Native 호환성 보장
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private WebSocketHandler webSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 순수 WebSocket 엔드포인트 설정 (STOMP 없음)
        // /ws와 /ws/ 둘 다 등록하여 trailing slash 유무와 관계없이 연결 허용
        registry.addHandler(webSocketHandler, "/ws", "/ws/")
                .setAllowedOrigins(
                    "https://cherrypick.co.kr",
                    "https://app.cherrypick.co.kr",
                    "http://localhost:3000",    // 개발환경용
                    "http://localhost:8081",    // React Native 개발환경
                    "*"                         // 개발 중 모든 origin 허용
                );
    }
}