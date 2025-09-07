package com.cherrypick.app.domain.websocket.event;

import org.springframework.context.ApplicationEvent;

/**
 * WebSocket 사용자 연결 이벤트
 */
public class UserConnectionEvent extends ApplicationEvent {
    
    private final Long userId;
    private final String sessionId;
    private final ConnectionEventType eventType;
    
    public UserConnectionEvent(Object source, Long userId, String sessionId, ConnectionEventType eventType) {
        super(source);
        this.userId = userId;
        this.sessionId = sessionId;
        this.eventType = eventType;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public ConnectionEventType getEventType() {
        return eventType;
    }
    
    public enum ConnectionEventType {
        CONNECTED, DISCONNECTED, ACTIVITY_UPDATE
    }
}