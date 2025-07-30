package com.cherrypick.app.domain.transaction.enums;

public enum TransactionStatus {
    PENDING,            // 대기 중
    SELLER_CONFIRMED,   // 판매자 확인됨
    BUYER_CONFIRMED,    // 구매자 확인됨
    COMPLETED,          // 완료됨
    CANCELLED,          // 취소됨
    DISPUTE             // 분쟁 상태
}