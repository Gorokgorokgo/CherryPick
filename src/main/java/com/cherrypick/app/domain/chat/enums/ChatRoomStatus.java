package com.cherrypick.app.domain.chat.enums;

/**
 * 채팅방 상태
 */
public enum ChatRoomStatus {
    /**
     * 비활성화 - 수수료 결제 대기 중
     */
    INACTIVE("비활성화"),
    
    /**
     * 활성화 - 채팅 가능
     */
    ACTIVE("활성화"),
    
    /**
     * 종료 - 거래 완료 또는 취소
     */
    CLOSED("종료");

    private final String description;

    ChatRoomStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}