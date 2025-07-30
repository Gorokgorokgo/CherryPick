package com.cherrypick.app.domain.bid;

public enum BidStatus {
    ACTIVE,     // 활성 상태
    CANCELLED,  // 취소됨
    FAILED,     // 실패 (시스템 오류 등)
    COMPLETED   // 완료됨 (낙찰 또는 경매 종료)
}