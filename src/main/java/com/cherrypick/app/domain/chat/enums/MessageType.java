package com.cherrypick.app.domain.chat.enums;

/**
 * 채팅 메시지 타입
 */
public enum MessageType {
    /**
     * 일반 텍스트 메시지
     */
    TEXT("텍스트"),
    
    /**
     * 이미지 메시지
     */
    IMAGE("이미지"),
    
    /**
     * 시스템 알림 메시지
     */
    SYSTEM("시스템");

    private final String description;

    MessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}