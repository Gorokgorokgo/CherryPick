package com.cherrypick.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 설정 클래스
 * STOMP 프로토콜을 사용한 실시간 통신 지원
 * 
 * 주요 기능:
 * - 실시간 입찰 업데이트
 * - 경매 상태 변경 알림
 * - 낙찰 알림
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트로 메시지를 전송할 때 사용할 prefix
        config.enableSimpleBroker("/topic", "/queue");
        
        // 클라이언트에서 서버로 메시지를 보낼 때 사용할 prefix  
        config.setApplicationDestinationPrefixes("/app");
        
        // 특정 사용자에게만 메시지를 보낼 때 사용할 prefix
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트 설정
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                    "https://cherrypick.co.kr", 
                    "https://app.cherrypick.co.kr",
                    "http://localhost:3000" // 개발환경용
                )
                .withSockJS(); // SockJS 지원 (WebSocket 미지원 브라우저 대응)
    }
}