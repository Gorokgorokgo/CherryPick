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
     * 경매 낙찰 (판매자용)
     */
    AUCTION_SOLD("경매 낙찰"),

    /**
     * 경매 유찰 (판매자용)
     */
    AUCTION_NOT_SOLD("경매 유찰"),

    /**
     * 경매 유찰 (최고 입찰자용)
     */
    AUCTION_NOT_SOLD_HIGHEST_BIDDER("경매 유찰 (최고 입찰)"),

    /**
     * 경매 종료 (일반 참여자용)
     */
    AUCTION_ENDED("경매 종료"),

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
    PROMOTION("프로모션"),

    /**
     * 경매 시간 연장 (스나이핑 방지)
     */
    AUCTION_EXTENDED("경매 시간 연장");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}