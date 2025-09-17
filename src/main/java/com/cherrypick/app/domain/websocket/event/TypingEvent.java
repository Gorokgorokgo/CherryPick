package com.cherrypick.app.domain.websocket.event;

import org.springframework.context.ApplicationEvent;

/**
 * 타이핑 상태 이벤트
 */
public class TypingEvent extends ApplicationEvent {
    
    private final Long chatRoomId;
    private final Long userId;
    private final String userNickname;
    private final TypingEventType eventType;
    
    public TypingEvent(Object source, Long chatRoomId, Long userId, String userNickname, TypingEventType eventType) {
        super(source);
        this.chatRoomId = chatRoomId;
        this.userId = userId;
        this.userNickname = userNickname;
        this.eventType = eventType;
    }
    
    public Long getChatRoomId() {
        return chatRoomId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public String getUserNickname() {
        return userNickname;
    }
    
    public TypingEventType getEventType() {
        return eventType;
    }
    
    public enum TypingEventType {
        START, STOP, MESSAGE_SENT
    }
}