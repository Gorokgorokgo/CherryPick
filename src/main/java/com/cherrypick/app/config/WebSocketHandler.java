package com.cherrypick.app.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import com.cherrypick.app.domain.websocket.event.UserConnectionEvent;
import com.cherrypick.app.domain.websocket.event.TypingEvent;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 순수 WebSocket 핸들러 (STOMP 없이)
 * React Native 호환성을 위한 JSON 메시지 기반 통신
 */
@Slf4j
@Component
public class WebSocketHandler extends TextWebSocketHandler {
    
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    
    public WebSocketHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        this.objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
    
    // 세션별 구독 경매 ID들 저장
    private final Map<String, Set<String>> sessionSubscriptions = new ConcurrentHashMap<>();
    
    // 경매별 구독자 세션들 저장 (auctionId -> Set<sessionId>)
    private final Map<String, Set<String>> auctionSubscribers = new ConcurrentHashMap<>();
    
    // 활성 WebSocket 세션들 (sessionId -> WebSocketSession)
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();
    
    // 세션별 사용자 ID 매핑 (sessionId -> userId)
    private final Map<String, Long> sessionUserMapping = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        sessionSubscriptions.put(sessionId, new CopyOnWriteArraySet<>());
        
        // 연결 확인 메시지 전송
        sendMessage(session, createConnectedMessage(sessionId));
        log.info("✅ WebSocket 연결 성공: {}", sessionId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        
        try {
            JsonNode messageNode = objectMapper.readTree(payload);
            String type = messageNode.has("type") ? messageNode.get("type").asText() : "";
            
            switch (type) {
                case "AUTH":
                    handleAuthentication(session, messageNode);
                    break;
                case "SUBSCRIBE":
                    handleSubscribe(session, messageNode);
                    break;
                case "UNSUBSCRIBE":
                    handleUnsubscribe(session, messageNode);
                    break;
                case "PING":
                    handlePing(session, messageNode);
                    break;
                case "TYPING_START":
                    handleTypingStart(session, messageNode);
                    break;
                case "TYPING_STOP":
                    handleTypingStop(session, messageNode);
                    break;
                default:
                    log.warn("⚠️ 알 수 없는 메시지 타입 [{}]: {}", sessionId, type);
                    sendErrorMessage(session, "UNKNOWN_MESSAGE_TYPE", "알 수 없는 메시지 타입: " + type);
            }
            
        } catch (Exception e) {
            log.error("❌ WebSocket 메시지 처리 오류 [{}]: {}", sessionId, payload, e);
            sendErrorMessage(session, "MESSAGE_PROCESSING_ERROR", "메시지 처리 중 오류가 발생했습니다");
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        log.info("🔚 WebSocket 연결 종료 [{}]: {}", sessionId, status.toString());
        
        // 사용자 연결 해제 이벤트 발행
        Long userId = sessionUserMapping.remove(sessionId);
        if (userId != null) {
            eventPublisher.publishEvent(new UserConnectionEvent(
                this, userId, sessionId, UserConnectionEvent.ConnectionEventType.DISCONNECTED
            ));
        }
        
        // 모든 구독 정보 정리
        Set<String> subscribedAuctions = sessionSubscriptions.remove(sessionId);
        if (subscribedAuctions != null) {
            subscribedAuctions.forEach(auctionId -> {
                Set<String> subscribers = auctionSubscribers.get(auctionId);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                    if (subscribers.isEmpty()) {
                        auctionSubscribers.remove(auctionId);
                    }
                }
            });
        }
        
        // 활성 세션에서 제거
        activeSessions.remove(sessionId);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("🚫 WebSocket 전송 오류 [{}]", sessionId, exception);
    }
    
    /**
     * 사용자 인증 처리
     */
    private void handleAuthentication(WebSocketSession session, JsonNode messageNode) {
        String sessionId = session.getId();
        
        if (!messageNode.has("userId")) {
            sendErrorMessage(session, "MISSING_USER_ID", "인증 요청에 userId가 필요합니다");
            return;
        }
        
        try {
            Long userId = messageNode.get("userId").asLong();
            
            // 세션-사용자 매핑 저장
            sessionUserMapping.put(sessionId, userId);
            
            // 사용자 연결 이벤트 발행
            eventPublisher.publishEvent(new UserConnectionEvent(
                this, userId, sessionId, UserConnectionEvent.ConnectionEventType.CONNECTED
            ));
            
            log.info("🔐 WebSocket 사용자 인증 완료 [{}]: userId={}", sessionId, userId);
            
            // 인증 성공 메시지 전송
            sendMessage(session, Map.of(
                "type", "AUTH_SUCCESS",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
            ));
            
        } catch (Exception e) {
            log.error("❌ WebSocket 사용자 인증 실패 [{}]", sessionId, e);
            sendErrorMessage(session, "AUTH_FAILED", "사용자 인증에 실패했습니다");
        }
    }

    /**
     * 구독 요청 처리
     */
    private void handleSubscribe(WebSocketSession session, JsonNode messageNode) {
        String sessionId = session.getId();
        
        if (!messageNode.has("auctionId")) {
            sendErrorMessage(session, "MISSING_AUCTION_ID", "구독 요청에 auctionId가 필요합니다");
            return;
        }
        
        String auctionId = messageNode.get("auctionId").asText();
        
        // 구독 정보 저장
        sessionSubscriptions.get(sessionId).add(auctionId);
        auctionSubscribers.computeIfAbsent(auctionId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        
        log.info("📡 구독 완료 [{}]: auction-{}", sessionId, auctionId);
        
        // 구독 확인 메시지 전송
        sendMessage(session, Map.of(
            "type", "SUBSCRIBED",
            "auctionId", auctionId,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * 구독 해제 요청 처리
     */
    private void handleUnsubscribe(WebSocketSession session, JsonNode messageNode) {
        String sessionId = session.getId();
        
        if (!messageNode.has("auctionId")) {
            sendErrorMessage(session, "MISSING_AUCTION_ID", "구독 해제 요청에 auctionId가 필요합니다");
            return;
        }
        
        String auctionId = messageNode.get("auctionId").asText();
        
        // 구독 정보 제거
        Set<String> subscribedAuctions = sessionSubscriptions.get(sessionId);
        if (subscribedAuctions != null) {
            subscribedAuctions.remove(auctionId);
        }
        
        Set<String> subscribers = auctionSubscribers.get(auctionId);
        if (subscribers != null) {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                auctionSubscribers.remove(auctionId);
            }
        }
        
        log.info("📡 구독 해제 완료 [{}]: auction-{}", sessionId, auctionId);
        
        // 구독 해제 확인 메시지 전송
        sendMessage(session, Map.of(
            "type", "UNSUBSCRIBED",
            "auctionId", auctionId,
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * PING 요청 처리 (하트비트)
     */
    private void handlePing(WebSocketSession session, JsonNode messageNode) {
        String sessionId = session.getId();
        long timestamp = messageNode.has("timestamp") ? messageNode.get("timestamp").asLong() : System.currentTimeMillis();
        
        // 사용자 활동 업데이트 이벤트 발행
        Long userId = sessionUserMapping.get(sessionId);
        if (userId != null) {
            eventPublisher.publishEvent(new UserConnectionEvent(
                this, userId, sessionId, UserConnectionEvent.ConnectionEventType.ACTIVITY_UPDATE
            ));
        }
        
        // PONG 응답 전송
        sendMessage(session, Map.of(
            "type", "PONG",
            "timestamp", timestamp,
            "serverTime", System.currentTimeMillis()
        ));
    }
    
    /**
     * 타이핑 시작 요청 처리
     */
    private void handleTypingStart(WebSocketSession session, JsonNode messageNode) {
        String sessionId = session.getId();
        Long userId = sessionUserMapping.get(sessionId);
        
        if (userId == null) {
            sendErrorMessage(session, "NOT_AUTHENTICATED", "인증이 필요합니다");
            return;
        }
        
        if (!messageNode.has("chatRoomId") || !messageNode.has("userNickname")) {
            sendErrorMessage(session, "MISSING_REQUIRED_FIELDS", "chatRoomId와 userNickname이 필요합니다");
            return;
        }
        
        try {
            Long chatRoomId = messageNode.get("chatRoomId").asLong();
            String userNickname = messageNode.get("userNickname").asText();
            
            eventPublisher.publishEvent(new TypingEvent(
                this, chatRoomId, userId, userNickname, TypingEvent.TypingEventType.START
            ));
            
            log.debug("타이핑 시작 처리: sessionId={}, userId={}, chatRoomId={}", sessionId, userId, chatRoomId);
            
        } catch (Exception e) {
            log.error("타이핑 시작 처리 오류 [{}]", sessionId, e);
            sendErrorMessage(session, "TYPING_START_ERROR", "타이핑 시작 처리 중 오류가 발생했습니다");
        }
    }
    
    /**
     * 타이핑 중단 요청 처리
     */
    private void handleTypingStop(WebSocketSession session, JsonNode messageNode) {
        String sessionId = session.getId();
        Long userId = sessionUserMapping.get(sessionId);
        
        if (userId == null) {
            sendErrorMessage(session, "NOT_AUTHENTICATED", "인증이 필요합니다");
            return;
        }
        
        if (!messageNode.has("chatRoomId")) {
            sendErrorMessage(session, "MISSING_CHAT_ROOM_ID", "chatRoomId가 필요합니다");
            return;
        }
        
        try {
            Long chatRoomId = messageNode.get("chatRoomId").asLong();
            
            eventPublisher.publishEvent(new TypingEvent(
                this, chatRoomId, userId, null, TypingEvent.TypingEventType.STOP
            ));
            
            log.debug("타이핑 중단 처리: sessionId={}, userId={}, chatRoomId={}", sessionId, userId, chatRoomId);
            
        } catch (Exception e) {
            log.error("타이핑 중단 처리 오류 [{}]", sessionId, e);
            sendErrorMessage(session, "TYPING_STOP_ERROR", "타이핑 중단 처리 중 오류가 발생했습니다");
        }
    }
    
    /**
     * 특정 경매 구독자들에게 메시지 전송 (기존 WebSocketMessagingService와 호환성)
     */
    public void sendToAuctionSubscribers(String destination, Object message) {
        // destination 형식:
        // "/topic/auctions/123" -> auctionId: "123"
        // "/topic/notifications/456" -> userId: "456" (알림용)

        if (destination.startsWith("/topic/notifications/")) {
            String userId = destination.substring("/topic/notifications/".length());
            sendToUser(userId, message);
        } else {
            String auctionId = extractAuctionId(destination);
            if (auctionId != null) {
                broadcastToAuction(auctionId, message);
            } else {
                log.warn("⚠️ 잘못된 destination 형식: {}", destination);
            }
        }
    }

    /**
     * 특정 사용자에게 메시지 전송 (알림용)
     */
    public void sendToUser(String userIdStr, Object message) {
        try {
            Long userId = Long.parseLong(userIdStr);

            // 해당 사용자의 모든 활성 세션 찾기
            int sentCount = 0;
            for (Map.Entry<String, Long> entry : sessionUserMapping.entrySet()) {
                if (entry.getValue().equals(userId)) {
                    WebSocketSession session = activeSessions.get(entry.getKey());
                    if (session != null && session.isOpen()) {
                        if (sendMessage(session, message)) {
                            sentCount++;
                        }
                    }
                }
            }

            if (sentCount > 0) {
                log.info("📤 사용자 {} 알림 전송 완료: {} 세션", userId, sentCount);
            } else {
                log.debug("사용자 {} 활성 세션 없음", userId);
            }

        } catch (NumberFormatException e) {
            log.error("잘못된 userId 형식: {}", userIdStr, e);
        }
    }
    
    /**
     * 특정 경매 구독자들에게 메시지 브로드캐스트
     */
    public void broadcastToAuction(String auctionId, Object message) {
        Set<String> subscriberIds = auctionSubscribers.get(auctionId);
        
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            log.warn("경매 {} 구독자가 없음", auctionId);
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (String sessionId : subscriberIds) {
            WebSocketSession session = activeSessions.get(sessionId);
            
            if (session != null && session.isOpen()) {
                if (sendMessage(session, message)) {
                    successCount++;
                } else {
                    failCount++;
                }
            } else {
                // 세션이 없거나 닫혀있는 경우 정리
                subscriberIds.remove(sessionId);
                failCount++;
            }
        }
        
        log.info("📤 경매 {} 브로드캐스트 완료: 성공 {}, 실패 {}", auctionId, successCount, failCount);
    }
    
    /**
     * 개별 세션에 메시지 전송
     */
    private boolean sendMessage(WebSocketSession session, Object message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(jsonMessage));
            return true;
        } catch (Exception e) {
            log.error("❌ 메시지 전송 실패 [{}]: {}", session.getId(), e.getMessage());
            return false;
        }
    }
    
    /**
     * 에러 메시지 전송
     */
    private void sendErrorMessage(WebSocketSession session, String errorCode, String errorMessage) {
        Map<String, Object> error = Map.of(
            "type", "ERROR",
            "code", errorCode,
            "message", errorMessage,
            "timestamp", System.currentTimeMillis()
        );
        sendMessage(session, error);
    }
    
    /**
     * 연결 확인 메시지 생성
     */
    private Map<String, Object> createConnectedMessage(String sessionId) {
        return Map.of(
            "type", "CONNECTED",
            "sessionId", sessionId,
            "timestamp", System.currentTimeMillis()
        );
    }
    
    /**
     * destination에서 auctionId 추출
     * "/topic/auctions/123" -> "123"
     */
    private String extractAuctionId(String destination) {
        if (destination != null && destination.startsWith("/topic/auctions/")) {
            return destination.substring("/topic/auctions/".length());
        }
        return null;
    }
    
    /**
     * 현재 활성 연결 수 조회 (모니터링용)
     */
    public int getActiveConnectionCount() {
        return activeSessions.size();
    }
    
    /**
     * 특정 경매의 구독자 수 조회 (모니터링용)
     */
    public int getAuctionSubscriberCount(String auctionId) {
        Set<String> subscribers = auctionSubscribers.get(auctionId);
        return subscribers != null ? subscribers.size() : 0;
    }
}