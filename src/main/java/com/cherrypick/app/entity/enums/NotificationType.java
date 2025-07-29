package com.cherrypick.app.entity.enums;

/**
 * 알림 유형 열거형
 */
public enum NotificationType {
    BID_NEW("새로운 입찰"),
    BID_OUTBID("입찰가 초과"),
    AUCTION_WIN("낙찰 성공"),
    AUCTION_LOSE("낙찰 실패"),
    AUCTION_END_SOON("경매 종료 임박"),
    AUCTION_REGION_EXPAND("지역 확장 제안"),
    CHAT_NEW_MESSAGE("새 메시지"),
    TRANSACTION_COMPLETE("거래 완료"),
    POINT_CHARGED("포인트 충전"),
    SYSTEM_NOTICE("시스템 공지");

    private final String description;

    NotificationType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}