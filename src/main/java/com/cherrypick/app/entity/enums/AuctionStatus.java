package com.cherrypick.app.entity.enums;

/**
 * 경매 상태 열거형
 */
public enum AuctionStatus {
    PENDING("대기"),
    ACTIVE("진행중"),
    COMPLETED("종료"),
    CANCELLED("취소");

    private final String description;

    AuctionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}