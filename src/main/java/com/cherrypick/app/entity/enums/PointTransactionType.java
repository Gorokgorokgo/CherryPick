package com.cherrypick.app.entity.enums;

/**
 * 포인트 거래 유형 열거형
 */
public enum PointTransactionType {
    CHARGE("충전"),
    DEPOSIT_LOCK("보증금 예치"),
    DEPOSIT_UNLOCK("보증금 해제"),
    BID_PAYMENT("낙찰 결제"),
    BID_REFUND("입찰 환불"),
    FEE_DEDUCTION("수수료 차감"),
    WITHDRAW("출금");

    private final String description;

    PointTransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}