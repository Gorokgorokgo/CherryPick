package com.cherrypick.app.domain.notification.enums;

/**
 * 알림 타입
 */
public enum NotificationType {
    /**
     * 새로운 입찰 (판매자용)
     */
    NEW_BID("새로운 입찰"),
    
    /**
     * 낙찰 알림 (구매자용)
     */
    AUCTION_WON("낙찰 알림"),
    
    /**
     * 연결 서비스 결제 요청 (판매자용)
     */
    CONNECTION_PAYMENT_REQUEST("연결 서비스 결제 요청"),
    
    /**
     * 채팅 활성화 (구매자용)
     */
    CHAT_ACTIVATED("채팅 활성화"),
    
    /**
     * 새 메시지
     */
    NEW_MESSAGE("새 메시지"),
    
    /**
     * 거래 완료
     */
    TRANSACTION_COMPLETED("거래 완료"),
    
    /**
     * 프로모션
     */
    PROMOTION("프로모션");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}