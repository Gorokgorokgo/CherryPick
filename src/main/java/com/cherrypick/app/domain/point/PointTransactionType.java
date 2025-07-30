package com.cherrypick.app.domain.point;

public enum PointTransactionType {
    CHARGE,         // 충전
    WITHDRAW,       // 출금
    USE,            // 사용
    REFUND,         // 환불
    DEPOSIT_LOCK,   // 보증금 잠금
    DEPOSIT_UNLOCK, // 보증금 해제
    BID_LOCK,       // 입찰 잠금
    BID_UNLOCK,     // 입찰 해제
    COMMISSION      // 수수료 차감
}