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
     * 거래 확인 대기 (상대방이 거래 확인 버튼을 눌렀을 때)
     */
    TRANSACTION_PENDING("거래 확인 대기"),

    /**
     * 거래 취소
     */
    TRANSACTION_CANCELLED("거래 취소"),
    
    /**
     * 프로모션
     */
    PROMOTION("프로모션"),

    /**
     * 경매 시간 연장 (스나이핑 방지)
     */
    AUCTION_EXTENDED("경매 시간 연장"),

    /**
     * 더 높은 입찰 발생 (이전 최고 입찰자에게)
     */
    OUTBID("더 높은 입찰 발생"),

    /**
     * 경매 마감 임박 (15분 전)
     */
    AUCTION_ENDING_SOON_15M("경매 마감 15분 전"),

    /**
     * 경매 마감 임박 (5분 전)
     */
    AUCTION_ENDING_SOON_5M("경매 마감 5분 전"),

    /**
     * 키워드 알림 (관심 키워드 경매 등록)
     */
    KEYWORD_ALERT("키워드 알림");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}