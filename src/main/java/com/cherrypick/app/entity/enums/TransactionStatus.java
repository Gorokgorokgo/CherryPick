package com.cherrypick.app.entity.enums;

/**
 * 거래 상태 열거형
 */
public enum TransactionStatus {
    PENDING("거래 대기"),
    IN_PROGRESS("거래 진행중"),
    COMPLETED("거래 완료"),
    CANCELLED("거래 취소"),
    DISPUTE("분쟁 중");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}