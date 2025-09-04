package com.cherrypick.app.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
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
    
    public WebSocketHandler() {
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
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        activeSessions.put(sessionId, session);
        sessionSubscriptions.put(sessionId, new CopyOnWriteArraySet<>());
        
        // 연결 확인 메시지 전송
        sendMessage(session, createConnectedMessage(sessionId));
        log.info("✅ WebSocket 연결 성공: {} [VERSION: 2024-09-04-DEBUG]", sessionId);
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        
        try {
            log.info("📩 WebSocket 메시지 수신 [{}]: {}", sessionId, payload);
            
            JsonNode messageNode = objectMapper.readTree(payload);
            String type = messageNode.has("type") ? messageNode.get("type").asText() : "";
            
            switch (type) {
                case "SUBSCRIBE":
                    handleSubscribe(session, messageNode);
                    break;
                case "UNSUBSCRIBE":
                    handleUnsubscribe(session, messageNode);
                    break;
                case "PING":
                    handlePing(session, messageNode);
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
        
        // 모든 구독 정보 정리
        Set<String> subscribedAuctions = sessionSubscriptions.remove(sessionId);
        if (subscribedAuctions != null) {
            subscribedAuctions.forEach(auctionId -> {
                Set<String> subscribers = auctionSubscribers.get(auctionId);
                if (subscribers != null) {
                    subscribers.remove(sessionId);
                    if (subscribers.isEmpty()) {
                        auctionSubscribers.remove(auctionId);
                        log.debug("경매 {} 구독자 목록 제거됨", auctionId);
                    }
                }
            });
        }
        
        // 활성 세션에서 제거
        activeSessions.remove(sessionId);
        
        log.debug("세션 정리 완료: {}", sessionId);
    }
    
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("🚫 WebSocket 전송 오류 [{}]", sessionId, exception);
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
        
        log.info("🔍 구독 처리 시작 - 세션: {}, 경매: {}", sessionId, auctionId);
        
        // 구독 정보 저장
        sessionSubscriptions.get(sessionId).add(auctionId);
        auctionSubscribers.computeIfAbsent(auctionId, k -> new CopyOnWriteArraySet<>()).add(sessionId);
        
        log.info("📊 구독 후 상태 - 경매 {} 구독자 수: {}", auctionId, auctionSubscribers.get(auctionId).size());
        log.info("📋 경매 {} 구독자 목록: {}", auctionId, auctionSubscribers.get(auctionId));
        
        log.info("📡 구독 완료 [{}]: auction-{} [DEBUG-TEST-2024]", sessionId, auctionId);
        
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
        long timestamp = messageNode.has("timestamp") ? messageNode.get("timestamp").asLong() : System.currentTimeMillis();
        
        // PONG 응답 전송
        sendMessage(session, Map.of(
            "type", "PONG",
            "timestamp", timestamp,
            "serverTime", System.currentTimeMillis()
        ));
    }
    
    /**
     * 특정 경매 구독자들에게 메시지 전송 (기존 WebSocketMessagingService와 호환성)
     */
    public void sendToAuctionSubscribers(String destination, Object message) {
        log.info("🎯 sendToAuctionSubscribers 호출 - destination: {}, message: {}", destination, message);
        // destination 형식: "/topic/auctions/123" -> auctionId: "123" 
        String auctionId = extractAuctionId(destination);
        log.info("🔍 추출된 auctionId: {}", auctionId);
        if (auctionId != null) {
            broadcastToAuction(auctionId, message);
        } else {
            log.warn("⚠️ 잘못된 destination 형식: {}", destination);
        }
    }
    
    /**
     * 특정 경매 구독자들에게 메시지 브로드캐스트
     */
    public void broadcastToAuction(String auctionId, Object message) {
        log.info("🚀 broadcastToAuction 호출 - auctionId: {}, message: {}", auctionId, message);
        log.info("🔍 현재 전체 구독 정보: {}", auctionSubscribers);
        
        Set<String> subscriberIds = auctionSubscribers.get(auctionId);
        log.info("📋 경매 {} 구독자 목록: {}", auctionId, subscriberIds);
        
        if (subscriberIds == null || subscriberIds.isEmpty()) {
            log.warn("경매 {} 구독자가 없음", auctionId);
            return;
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (String sessionId : subscriberIds) {
            log.info("🔍 세션 {} 처리 시도", sessionId);
            WebSocketSession session = activeSessions.get(sessionId);
            
            if (session != null && session.isOpen()) {
                log.info("✅ 세션 {} 활성화됨, 메시지 전송 시도", sessionId);
                if (sendMessage(session, message)) {
                    successCount++;
                } else {
                    failCount++;
                }
            } else {
                log.warn("❌ 세션 {} 비활성화 또는 닫힘", sessionId);
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
            log.debug("📤 전송할 JSON 메시지: {}", jsonMessage);
            session.sendMessage(new TextMessage(jsonMessage));
            log.debug("✅ 세션 {}에 메시지 전송 성공", session.getId());
            return true;
        } catch (Exception e) {
            log.error("❌ 메시지 전송 실패 [{}]: 메시지={}, 오류={}", session.getId(), message, e.getMessage(), e);
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