package com.cherrypick.app.domain.point;

public enum PointTransactionStatus {
    PENDING,    // 대기 중
    COMPLETED,  // 완료됨
    FAILED,     // 실패
    CANCELLED   // 취소됨
}